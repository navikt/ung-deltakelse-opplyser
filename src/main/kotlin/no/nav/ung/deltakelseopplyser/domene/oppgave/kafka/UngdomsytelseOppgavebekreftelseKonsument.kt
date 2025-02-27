package no.nav.ung.deltakelseopplyser.domene.oppgave.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.ung.deltakelseopplyser.config.TxConfiguration.Companion.TRANSACTION_MANAGER
import no.nav.ung.deltakelseopplyser.domene.oppgave.OppgaveService
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
class UngdomsytelseOppgavebekreftelseKonsument(
    private val objectMapper: ObjectMapper,
    private val oppgaveService: OppgaveService,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(UngdomsytelseOppgavebekreftelseKonsument::class.java)
    }

    @Transactional(TRANSACTION_MANAGER)
    @KafkaListener(
        topics = ["#{'\${topic.listener.ung-oppgavebekreftelse.navn}'}"],
        id = "#{'\${topic.listener.ung-oppgavebekreftelse.id}'}",
        groupId = "#{'\${spring.kafka.consumer.group-id}'}",
        autoStartup = "#{'\${topic.listener.ung-oppgavebekreftelse.bryter}'}",
        filter = UNGDOMSYTELSESØKNAD_FILTER,
        properties = [
            "auto.offset.reset=#{'\${topic.listener.ung-soknad.auto-offset-reset}'}"
        ]
    )
    fun konsumer(
        @Payload ungdomsytelseOppgavebekreftelseTopicEntry: String,
    ) {
        val oppgavebekreftelseTopicEntry =
            objectMapper.readValue(ungdomsytelseOppgavebekreftelseTopicEntry, UngdomsytelseOppgavebekreftelseTopicEntry::class.java)
        logger.info("Mottar og håndterer UngdomsytelseOppgavebekreftelse for ungdomsytelsen")
        oppgaveService.håndterMottattOppgavebekreftelse(oppgavebekreftelseTopicEntry.data.journalførtMelding)
        logger.info("Håndtert UngdomsytelseOppgavebekreftelse for ungdomsytelsen.")
    }
}

@Configuration
class UngdomsytelseOppgavebekreftelseKonsumentConfiguration(
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(UngdomsytelseOppgavebekreftelseKonsumentConfiguration::class.java)
        const val UNGDOMSYTELSESOPPGAVEBEKREFTELSE_FILTER = "ungdomsytelsesOppgavebekreftelseFilter"
    }

    @Bean(UNGDOMSYTELSESOPPGAVEBEKREFTELSE_FILTER)
    fun ungdomsytelseOppgavebekreftelseFilter() = RecordFilterStrategy<String, String> { consumerRecord ->
        try {
            val oppgavebekreftelseTopicEntry = objectMapper.readValue(consumerRecord.value(), UngdomsytelseOppgavebekreftelseTopicEntry::class.java)
            MDCUtil.toMDC(Constants.CORRELATION_ID, oppgavebekreftelseTopicEntry.metadata.correlationId)
            MDCUtil.toMDC(Constants.JOURNALPOST_ID, oppgavebekreftelseTopicEntry.data.journalførtMelding.journalpostId)
            logger.info("Deserialisert UngdomsytelseOppgavebekreftelse fra topic")
            false
        } catch (e: Exception) {
            logger.error("Kunne ikke deserialisere UngdomsytelseOppgavebekreftelse fra topic", e)
            throw e
        }
    }
}
