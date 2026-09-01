package com.hanati.web3.batch_decoder_demo.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "decoder")
data class DecoderProperties(
    val l1Rpc: String,
    val l1Beacon: String,
    val inbox: String,
    val sender: String,
)
