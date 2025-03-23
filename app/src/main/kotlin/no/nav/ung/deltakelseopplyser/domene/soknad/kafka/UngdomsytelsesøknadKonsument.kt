package no.nav.ung.deltakelseopplyser.domene.soknad.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.ung.deltakelseopplyser.config.TxConfiguration.Companion.TRANSACTION_MANAGER
import no.nav.ung.deltakelseopplyser.domene.soknad.UngdomsytelsesøknadService
import no.nav.ung.deltakelseopplyser.domene.soknad.kafka.UngdomsytelsesøknadKonsumentConfiguration.Companion.UNGDOMSYTELSESØKNAD_FILTER
import no.nav.ung.deltakelseopplyser.utils.Constants
import no.nav.ung.deltakelseopplyser.utils.MDCUtil
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.adapter.RecordFilterStrategy
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UngdomsytelsesøknadKonsument(
    private val objectMapper: ObjectMapper,
    private val ungdomsytelsesøknadService: UngdomsytelsesøknadService,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(UngdomsytelsesøknadKonsument::class.java)
    }

    @Transactional(TRANSACTION_MANAGER)
    @KafkaListener(
        topics = ["#{'\${topic.listener.ung-soknad.navn}'}"],
        id = "#{'\${topic.listener.ung-soknad.id}'}",
        groupId = "#{'\${spring.kafka.consumer.group-id}'}",
        autoStartup = "#{'\${topic.listener.ung-soknad.bryter}'}",
        filter = UNGDOMSYTELSESØKNAD_FILTER,
        properties = [
            "auto.offset.reset=#{'\${topic.listener.ung-soknad.auto-offset-reset}'}"
        ]
    )
    fun konsumer(
        @Payload ungdomsytelseSøknadTopicEntry: String,
    ) {
        val søknadTopicEntry =
            objectMapper.readValue(ungdomsytelseSøknadTopicEntry, UngdomsytelseSøknadTopicEntry::class.java)
        logger.info("Mottar og håndterer søknad for ungdomsytelsen")
        ungdomsytelsesøknadService.håndterMottattSøknad(søknadTopicEntry.data.journalførtMelding)
        logger.info("Håndtert søknad for ungdomsytelsen.")
    }
}

@Configuration
class UngdomsytelsesøknadKonsumentConfiguration(
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(UngdomsytelsesøknadKonsumentConfiguration::class.java)
        const val UNGDOMSYTELSESØKNAD_FILTER = "ungdomsytelsesøknadFilter"
    }

    @Bean(UNGDOMSYTELSESØKNAD_FILTER)
    fun ungdomsytelsesøknadFilter() = RecordFilterStrategy<String, String> { consumerRecord ->
        try {
            val søknadTopicEntry = objectMapper.readValue(consumerRecord.value(), UngdomsytelseSøknadTopicEntry::class.java)
            MDCUtil.toMDC(Constants.CORRELATION_ID, søknadTopicEntry.metadata.correlationId)
            MDCUtil.toMDC(Constants.JOURNALPOST_ID, søknadTopicEntry.data.journalførtMelding.journalpostId)
            logger.info("Deserialisert ungdomsytelsesøknad fra topic")
            false
        } catch (e: Exception) {
            logger.error("Kunne ikke deserialisere melding fra topic", e)
            throw e
        }
    }
}
