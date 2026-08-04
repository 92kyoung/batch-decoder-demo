package com.hanati.web3.batch_decoder_demo

import com.hanati.web3.batch_decoder_demo.adapter.out.ProcessBuilderBatchDecoderAdapter
import com.hanati.web3.batch_decoder_demo.config.DecoderProperties
import org.junit.jupiter.api.Test

/**
 * Plain (non-Spring) manual smoke test - constructs the adapter directly so it doesn't need the
 * JPA/DataSource context to be configured, and hits real L1/beacon endpoints.
 */
class BatchDecoderServiceManualTest {

    @Test
    fun `fetchAndReassemble against GIWA Sepolia block range`() {
        val adapter = ProcessBuilderBatchDecoderAdapter(
            DecoderProperties(
                l1Rpc = "https://eth-sepolia.g.alchemy.com/v2/uKKRn-W7U18EAYoK66WDI9JhdLobWSll",
                l1Beacon = "https://eth-sepoliabeacon.g.alchemy.com/v2/uKKRn-W7U18EAYoK66WDI9JhdLobWSll",
                inbox = "0x00Ef2E3b7754F2A65f1E897a27A3306d9b52f544",
                sender = "0x1cAAaa58002a7e8B4c6f427ac2c943767b4d6cd7",
            ),
        )

        val channels = adapter.fetchAndReassemble(11378985, 11379085)

        println("channels found: ${channels.size}")
        val targetTx = "0xee0e88d1c00649d167032b68a9a9e238b337bfe53030f70c2281661d87729e1d"
        for (ch in channels) {
            val hasTarget = ch.frames.map { it.transactionHash }.contains(targetTx)
            println(
                "id=${ch.id} is_ready=${ch.isReady} " +
                    "invalid_batches=${ch.invalidBatches} frames=${ch.frames.size} " +
                    "containsTargetTx=$hasTarget"
            )
        }
    }
}
