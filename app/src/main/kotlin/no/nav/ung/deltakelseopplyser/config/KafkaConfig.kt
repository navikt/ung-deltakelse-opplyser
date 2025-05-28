package no.nav.ung.deltakelseopplyser.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries


@Configuration
class KafkaConfig {

    @Bean
    fun defaultErrorHandler(): DefaultErrorHandler {
        val bo = ExponentialBackOffWithMaxRetries(Int.MAX_VALUE)
        return DefaultErrorHandler(bo)
    }
}
