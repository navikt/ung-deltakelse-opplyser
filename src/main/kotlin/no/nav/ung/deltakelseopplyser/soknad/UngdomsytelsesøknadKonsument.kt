package no.nav.ung.deltakelseopplyser.soknad

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
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
