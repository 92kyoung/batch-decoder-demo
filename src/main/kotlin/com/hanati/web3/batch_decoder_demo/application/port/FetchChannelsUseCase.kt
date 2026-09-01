package com.hanati.web3.batch_decoder_demo.application.port

import com.hanati.web3.batch_decoder_demo.domain.Channel

/** Inbound port - what driving adapters (controllers, CLI, tests, ...) call into. */
interface FetchChannelsUseCase {
    fun fetchChannels(start: Long, end: Long): List<Channel>
}
