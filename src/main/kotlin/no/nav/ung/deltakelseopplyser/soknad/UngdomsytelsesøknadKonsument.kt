package no.nav.ung.deltakelseopplyser.soknad

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

@Service
class UngdomsytelsesøknadKonsument(
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(UngdomsytelsesøknadKonsument::class.java)
    }

    @KafkaListener(
        topics = ["#{'\${topic.listener.ung-soknad.navn}'}"],
        id = "#{'\${topic.listener.ung-soknad.id}'}",
        groupId = "#{'\${spring.kafka.consumer.group-id}'}",
        autoStartup = "#{'\${topic.listener.ung-soknad.bryter}'}",
        properties = ["auto.offset.reset: latest"]
    )
    fun konsumer(
        @Payload ungdomsytelseSøknadTopicEntry: String,
    ) {
        val søknadPrettyJson =
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(ungdomsytelseSøknadTopicEntry)
        logger.info("Leser melding fra topic: {}", søknadPrettyJson)
    }
}
