package no.nav.ung.deltakelseopplyser.soknad

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.ung.deltakelseopplyser.utils.Constants
import no.nav.ung.deltakelseopplyser.utils.MDCUtil
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.adapter.RecordFilterStrategy
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

@Service
class UngdomsytelsesøknadKonsument(
    private val objectMapper: ObjectMapper,
    private val ungdomsytelsesøknadService: UngdomsytelsesøknadService,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(UngdomsytelsesøknadKonsument::class.java)
    }

    @KafkaListener(
        topics = ["#{'\${topic.listener.ung-soknad.navn}'}"],
        id = "#{'\${topic.listener.ung-soknad.id}'}",
        groupId = "#{'\${spring.kafka.consumer.group-id}'}",
        autoStartup = "#{'\${topic.listener.ung-soknad.bryter}'}",
        filter = "ungdomsytelsesøknadFilter",
        properties = [
            "auto.offset.reset=#{'\${topic.listener.ung-soknad.auto-offset-reset}'}"
        ]
    )
    fun konsumer(
        @Payload ungdomsytelseSøknadTopicEntry: String,
    ) {
        val søknadTopicEntry =
            objectMapper.readValue(ungdomsytelseSøknadTopicEntry, UngdomsytelseSøknadTopicEntry::class.java)
        logger.info("Deserialisert melding fra topic: {}", søknadTopicEntry)
        ungdomsytelsesøknadService.håndterMottattSøknad(søknadTopicEntry.data.journalførtMelding)
        logger.info("Håndtert søknad fra topic")
    }
}

@Configuration
class UngdomsytelsesøknadKonsumentConfiguration(
    private val objectMapper: ObjectMapper,
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(UngdomsytelsesøknadKonsumentConfiguration::class.java)
    }

    @Bean("ungdomsytelsesøknadFilter")
    fun ungdomsytelsesøknadFilter() = RecordFilterStrategy<String, String> { consumerRecord ->
        try {
            val readValue = objectMapper.readValue(consumerRecord.value(), UngdomsytelseSøknadTopicEntry::class.java)
            logger.info("FILTER --> Deserialisert melding fra topic: {}", readValue)
            MDCUtil.toMDC(Constants.CORRELATION_ID, readValue.metadata.correlationId)
            false
        } catch (e: Exception) {
            logger.error("Kunne ikke deserialisere melding fra topic", e)
            false
        }
    }
}
