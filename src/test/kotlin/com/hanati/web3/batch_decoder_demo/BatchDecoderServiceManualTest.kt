package com.hanati.web3.batch_decoder_demo

import org.junit.jupiter.api.Test

/**
 * Plain (non-Spring) manual smoke test - constructs the service directly so it doesn't need the
 * JPA/DataSource context to be configured, and hits real L1/beacon endpoints.
 */
class BatchDecoderServiceManualTest {

    @Test
    fun `fetchAndReassemble against GIWA Sepolia block range`() {
        val service = BatchDecoderService(
            l1Rpc = "https://eth-sepolia.g.alchemy.com/v2/uKKRn-W7U18EAYoK66WDI9JhdLobWSll",
            l1Beacon = "https://eth-sepoliabeacon.g.alchemy.com/v2/uKKRn-W7U18EAYoK66WDI9JhdLobWSll",
            inboxAddress = "0x00Ef2E3b7754F2A65f1E897a27A3306d9b52f544",
            senderAddress = "0x1cAAaa58002a7e8B4c6f427ac2c943767b4d6cd7",
        )

        val channels = service.fetchAndReassemble(11378985, 11379085)

        println("channels found: ${channels.size}")
        val targetTx = "0xee0e88d1c00649d167032b68a9a9e238b337bfe53030f70c2281661d87729e1d"
        for (ch in channels) {
            val frameHashes = ch["frames"].map { it["transaction_hash"]?.asText() ?: "" }
            val hasTarget = frameHashes.contains(targetTx)
            println(
                "id=${ch["id"].asText()} is_ready=${ch["is_ready"].asBoolean()} " +
                    "invalid_batches=${ch["invalid_batches"].asBoolean()} frames=${ch["frames"].size()} " +
                    "containsTargetTx=$hasTarget"
            )
        }
    }
}
