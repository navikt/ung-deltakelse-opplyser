package no.nav.ung.deltakelseopplyser.domene.inntekt.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.ung.deltakelseopplyser.config.TxConfiguration.Companion.TRANSACTION_MANAGER
import no.nav.ung.deltakelseopplyser.domene.inntekt.RapportertInntektHåndtererService
import no.nav.ung.deltakelseopplyser.domene.inntekt.kafka.UngdomsytelseRapportertInntektKonsumentConfiguration.Companion.UNGDOMSYTELSE_RAPPORTERT_INNTEKT_FILTER
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
class UngdomsytelseRapportertInntektKonsument(
    private val objectMapper: ObjectMapper,
    private val rapportertInntektHåndtererService: RapportertInntektHåndtererService,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(UngdomsytelseRapportertInntektKonsument::class.java)
    }


    @Transactional(TRANSACTION_MANAGER)
    @KafkaListener(
        topics = ["#{'\${topic.listener.ung-rapportert-inntekt.navn}'}"],
        id = "#{'\${topic.listener.ung-rapportert-inntekt.id}'}",
        groupId = "#{'\${spring.kafka.consumer.group-id}'}",
        autoStartup = "#{'\${topic.listener.ung-rapportert-inntekt.bryter}'}",
        filter = UNGDOMSYTELSE_RAPPORTERT_INNTEKT_FILTER,
        properties = [
            "auto.offset.reset=#{'\${topic.listener.ung-rapportert-inntekt.auto-offset-reset}'}"
        ]
    )
    fun konsumer(
        @Payload ungdomsytelseRapportertInntektTopicEntry: String,
    ) {
        val rapportertInntektTopicEntry =
            objectMapper.readValue(
                ungdomsytelseRapportertInntektTopicEntry,
                UngdomsytelseRapportertInntektTopicEntry::class.java
            )
        logger.info("Mottar og håndterer UngdomsytelseRapportertInntekt for ungdomsytelsen")
        rapportertInntektHåndtererService.håndterRapportertInntekt(rapportertInntektTopicEntry.data.journalførtMelding)
        logger.info("Håndtert UngdomsytelseRapportertInntekt for ungdomsytelsen.")
    }
}

@Configuration
class UngdomsytelseRapportertInntektKonsumentConfiguration(
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val logger = LoggerFactory.getLogger(UngdomsytelseRapportertInntektKonsumentConfiguration::class.java)
        internal const val UNGDOMSYTELSE_RAPPORTERT_INNTEKT_FILTER = "ungdomsytelsesRapportertInntektFilter"
    }

    @Bean(UNGDOMSYTELSE_RAPPORTERT_INNTEKT_FILTER)
    fun ungdomsytelsesRapportertInntektFilter() = RecordFilterStrategy<String, String> { consumerRecord ->
        try {
            val rapportertInntektTopicEntry =
                objectMapper.readValue(consumerRecord.value(), UngdomsytelseRapportertInntektTopicEntry::class.java)
            MDCUtil.toMDC(Constants.CORRELATION_ID, rapportertInntektTopicEntry.metadata.correlationId)
            MDCUtil.toMDC(Constants.JOURNALPOST_ID, rapportertInntektTopicEntry.data.journalførtMelding.journalpostId)
            logger.info("Deserialisert UngdomsytelseRapportertInntekt fra topic")
            false
        } catch (e: Exception) {
            logger.error("Kunne ikke deserialisere UngdomsytelseRapportertInntekt fra topic", e)
            throw e
        }
    }
}
