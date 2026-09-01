package com.hanati.web3.batch_decoder_demo.adapter.`in`

import com.hanati.web3.batch_decoder_demo.application.port.FetchChannelsUseCase
import com.hanati.web3.batch_decoder_demo.domain.Channel
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class FetchChannelsController(
    private val fetchChannelsUseCase: FetchChannelsUseCase,
) {
    @GetMapping("/api/channels")
    fun fetchChannels(
        @RequestParam start: Long,
        @RequestParam end: Long,
    ): List<Channel> = fetchChannelsUseCase.fetchChannels(start, end)
}
