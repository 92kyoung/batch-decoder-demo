package com.hanati.web3.batch_decoder_demo.adapter.out

import com.hanati.web3.batch_decoder_demo.application.port.BatchDecoderPort
import com.hanati.web3.batch_decoder_demo.config.DecoderProperties
import com.hanati.web3.batch_decoder_demo.domain.Channel
import com.hanati.web3.batch_decoder_demo.domain.Frame
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Wraps the batch_decoder binary (bundled as a JAR resource under /bin) with ProcessBuilder so
 * the rest of the app can call fetch+reassemble like a normal function, without depending on Go
 * or this repo being present on the machine the app runs on.
 */
@Component
class ProcessBuilderBatchDecoderAdapter(
    decoderProperties: DecoderProperties,
) : BatchDecoderPort {
    private val l1Rpc = decoderProperties.l1Rpc
    private val l1Beacon = decoderProperties.l1Beacon
    private val inboxAddress = decoderProperties.inbox
    private val senderAddress = decoderProperties.sender

    private val objectMapper = ObjectMapper()

    // Extracted once per JVM lifetime and reused - these two files never change between calls.
    private val binaryPath: Path by lazy { extractResource(binaryResourceName(), executable = true) }
    private val rollupConfigPath: Path by lazy { extractResource("rollup.json", executable = false) }

    // Persistent across calls (and process restarts) - channel JSON accumulates here instead of
    // being thrown away, so past decode results stay available on disk.
    private val channelCacheDir: Path by lazy {
        Paths.get("channel_cache").toAbsolutePath().also { Files.createDirectories(it) }
    }

    private fun binaryResourceName(): String {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") -> "batch_decoder-windows.exe"
            os.contains("nix") || os.contains("nux") -> "batch_decoder-linux-amd64"
            else -> throw UnsupportedOperationException("unsupported OS: $os")
        }
    }

    private fun extractResource(resourceName: String, executable: Boolean): Path {
        val target = Files.createTempFile("batch_decoder_", "_$resourceName")
        javaClass.getResourceAsStream("/bin/$resourceName")?.use { input ->
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
        } ?: throw FileNotFoundException("classpath resource not found: /bin/$resourceName")
        if (executable) target.toFile().setExecutable(true)
        target.toFile().deleteOnExit()
        return target
    }

    /**
     * Fetches and reassembles L1 blocks [start, end), returning one decoded channel per element
     * (same shape as batch_decoder's channel_cache/<channelID>.json files).
     */
    override fun fetchAndReassemble(start: Long, end: Long): List<Channel> {
        val workDir = Files.createTempDirectory("batch_decoder_run_")
        val txCacheDir = workDir.resolve("tx_cache").also { Files.createDirectories(it) }
        val runStart = Instant.now().minusSeconds(1)

        try {
            // The L1 beacon endpoint drops connections under load fairly often (observed
            // repeatedly in practice) - fetch is safe to retry as-is, it just re-writes
            // tx_cache/<txhash>.json each time. reassemble is pure local computation, so a
            // failure there is a real bug and isn't retried.
            runDecoderWithRetry(
                maxAttempts = 3,
                "fetch",
                "--l1", l1Rpc,
                "--l1.beacon", l1Beacon,
                "--inbox", inboxAddress,
                "--sender", senderAddress,
                "--start", start.toString(),
                "--end", end.toString(),
                "--concurrent-requests", "2",
                "--out", txCacheDir.toString(),
            )

            runDecoder(
                "reassemble",
                "--in", txCacheDir.toString(),
                "--out", channelCacheDir.toString(),
                "--rollup-config", rollupConfigPath.toString(),
            )

            return Files.list(channelCacheDir).use { files ->
                files.filter { it.toString().endsWith(".json") && Files.getLastModifiedTime(it).toInstant() >= runStart }
                    .map { parseChannel(objectMapper.readTree(it.toFile())) }
                    .toList()
            }
        } finally {
            workDir.toFile().deleteRecursively()
        }
    }

    private fun parseChannel(node: JsonNode): Channel {
        val frames = mutableListOf<Frame>()
        for (frame in node["frames"]) {
            frames += Frame(transactionHash = frame["transaction_hash"]?.asText())
        }
        return Channel(
            id = node["id"].asText(),
            isReady = node["is_ready"].asBoolean(),
            invalidBatches = node["invalid_batches"].asBoolean(),
            frames = frames,
        )
    }

    private fun runDecoderWithRetry(maxAttempts: Int, vararg args: String) {
        var lastError: Exception? = null
        repeat(maxAttempts) { attempt ->
            try {
                runDecoder(*args)
                return
            } catch (e: Exception) {
                lastError = e
                println("batch_decoder ${args.firstOrNull()} attempt ${attempt + 1}/$maxAttempts failed: ${e.message}")
                if (attempt < maxAttempts - 1) {
                    Thread.sleep(2_000L * (attempt + 1))
                }
            }
        }
        throw RuntimeException("batch_decoder ${args.firstOrNull()} failed after $maxAttempts attempts", lastError)
    }

    private fun runDecoder(vararg args: String) {
        val process = ProcessBuilder(binaryPath.toString(), *args)
            .redirectErrorStream(false)
            .start()

        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()

        val finished = process.waitFor(5, TimeUnit.MINUTES)
        if (!finished) {
            process.destroyForcibly()
            throw RuntimeException("batch_decoder ${args.firstOrNull()} timed out after 5m")
        }
        if (process.exitValue() != 0) {
            throw RuntimeException("batch_decoder ${args.firstOrNull()} failed (exit ${process.exitValue()}): $stderr")
        }
        if (stdout.isNotBlank()) {
            println(stdout)
        }
    }
}
