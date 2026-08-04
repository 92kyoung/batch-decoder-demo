package com.hanati.web3.batch_decoder_demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.runApplication

// JPA/DataSource autoconfig disabled - no entities/repositories or DB configured yet, and this
// demo doesn't need one to exercise the decoder over HTTP. Re-enable once a real DB is wired up.
@SpringBootApplication(exclude = [DataSourceAutoConfiguration::class, HibernateJpaAutoConfiguration::class])
@ConfigurationPropertiesScan
class BatchDecoderDemoApplication

fun main(args: Array<String>) {
	runApplication<BatchDecoderDemoApplication>(*args)
}
