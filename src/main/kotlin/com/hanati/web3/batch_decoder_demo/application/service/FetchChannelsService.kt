package com.hanati.web3.batch_decoder_demo.application.service

import com.hanati.web3.batch_decoder_demo.application.port.BatchDecoderPort
import com.hanati.web3.batch_decoder_demo.application.port.FetchChannelsUseCase
import com.hanati.web3.batch_decoder_demo.domain.Channel
import org.springframework.stereotype.Service

@Service
class FetchChannelsService(
    private val batchDecoderPort: BatchDecoderPort,
) : FetchChannelsUseCase {
    override fun fetchChannels(start: Long, end: Long): List<Channel> =
        batchDecoderPort.fetchAndReassemble(start, end)
}
