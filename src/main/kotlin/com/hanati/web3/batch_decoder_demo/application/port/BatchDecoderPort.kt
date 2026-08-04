package com.hanati.web3.batch_decoder_demo.application.port

import com.hanati.web3.batch_decoder_demo.domain.Channel

/** Outbound port - how the application reaches out to whatever actually decodes L1 batches. */
interface BatchDecoderPort {
    fun fetchAndReassemble(start: Long, end: Long): List<Channel>
}
