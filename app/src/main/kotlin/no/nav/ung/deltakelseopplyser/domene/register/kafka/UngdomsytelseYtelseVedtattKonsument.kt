package no.nav.ung.deltakelseopplyser.domene.register.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.abakus.vedtak.ytelse.v1.YtelseV1
import no.nav.ung.deltakelseopplyser.config.TxConfiguration.Companion.TRANSACTION_MANAGER
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramytelseVedtakService
import no.nav.ung.deltakelseopplyser.domene.register.kafka.UngdomsytelseVedtakFattetKonsumentConfiguration.Companion.OPPHØRSVEDTAK_FILTER
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.adapter.RecordFilterStrategy
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class UngdomsytelseYtelseVedtattKonsument(
    private val objectMapper: ObjectMapper,
    private val ungdomsprogramytelseVedtakService: UngdomsprogramytelseVedtakService,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(UngdomsytelseYtelseVedtattKonsument::class.java)
    }

    @Transactional(TRANSACTION_MANAGER)
    @KafkaListener(
        topics = ["#{'\${topic.listener.ung-vedtakhendelse.navn}'}"],
        id = "#{'\${topic.listener.ung-vedtakhendelse.id}'}",
        groupId = "#{'\${spring.kafka.consumer.group-id}'}",
        autoStartup = "#{'\${topic.listener.ung-oppgavebekreftelse.bryter}'}",
        filter = OPPHØRSVEDTAK_FILTER,
        properties = [
            "auto.offset.reset=#{'\${topic.listener.ung-vedtakhendelse.auto-offset-reset}'}"
        ]
    )
    fun konsumer(
        @Payload ungdomsytelseYtelseVedtattTopicEntry: String,
    ) {
        val vedtakTopicEntry =
            objectMapper.readValue(
                ungdomsytelseYtelseVedtattTopicEntry,
                no.nav.abakus.vedtak.ytelse.Ytelse::class.java
            )
        logger.info("Mottar og håndterer opphørsvedtak for ungdomsytelsen")
        ungdomsprogramytelseVedtakService.håndterUngdomsprogramytelseOpphørsvedtakForAktør(vedtakTopicEntry.aktør.verdi)
        logger.info("Håndtert opphørsvedtak for ungdomsytelsen.")
    }
}

@Configuration
class UngdomsytelseVedtakFattetKonsumentConfiguration(
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(UngdomsytelseVedtakFattetKonsumentConfiguration::class.java)
        internal const val OPPHØRSVEDTAK_FILTER = "opphoersvedtakFilter"
    }

    @Bean(OPPHØRSVEDTAK_FILTER)
    fun opphørsvedtakFilter() = RecordFilterStrategy<String, String> { consumerRecord ->
        try {
            val mottattVedtak =
                objectMapper.readValue(consumerRecord.value(), no.nav.abakus.vedtak.ytelse.Ytelse::class.java)
            logger.info("Deserialisert vedtak fra topic for key ${consumerRecord.key()}")
            val mottattVedtak1 = mottattVedtak as YtelseV1
            mottattVedtak1.anvist?.isEmpty() != false || mottattVedtak1.anvist?.all { it.dagsats.verdi.compareTo(BigDecimal.ZERO) == 0 } == true
        } catch (e: Exception) {
            logger.error("Kunne ikke deserialisere UngdomsytelseOppgavebekreftelse fra topic for key ${consumerRecord.key()}", e)
            throw e
        }
    }
}
