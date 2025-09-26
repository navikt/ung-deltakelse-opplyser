package no.nav.ung.deltakelseopplyser.outbox

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class OutboxProducer(
    private val outboxRepository: OutboxRepository
) {
    private val logger = LoggerFactory.getLogger(OutboxProducer::class.java)

    fun publishEvent(event: OutboxEvent) {
        logger.info("Publishing event of type {} for aggregate {}", event.eventType, event.aggregateId)

        val outboxEntry = OutboxDAO(
            eventType = event.eventType,
            aggregateId = event.aggregateId,
            payload = event.toJson()
        )

        outboxRepository.save(outboxEntry)
        logger.debug("Event saved to outbox with id {}", outboxEntry.id)
    }

    fun publishEvents(events: List<OutboxEvent>) {
        if (events.isEmpty()) return

        logger.info("Publishing {} events", events.size)

        val outboxEntries = events.map { event ->
            OutboxDAO(
                eventType = event.eventType,
                aggregateId = event.aggregateId,
                payload = event.toJson()
            )
        }

        outboxRepository.saveAll(outboxEntries)
        logger.debug("Saved {} events to outbox", outboxEntries.size)
    }
}
