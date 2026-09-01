package com.hanati.web3.batch_decoder_demo.domain

data class Channel(
    val id: String,
    val isReady: Boolean,
    val invalidBatches: Boolean,
    val frames: List<Frame>,
)

data class Frame(
    val transactionHash: String?,
)
