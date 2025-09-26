package no.nav.ung.deltakelseopplyser.outbox

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class OutboxConsumer(
    private val outboxRepository: OutboxRepository,
    private val eventHandlers: List<OutboxEventHandler>
) {
    private val logger = LoggerFactory.getLogger(OutboxConsumer::class.java)
    private val handlerMap: Map<String, OutboxEventHandler> = eventHandlers.associateBy { it.eventType }

    fun processEvents(batchSize: Int = 10): Int {
        val events = outboxRepository.findPendingEventsWithLimit(LocalDateTime.now(), batchSize)

        if (events.isEmpty()) {
            return 0
        }

        logger.info("Processing {} outbox events", events.size)
        var processedCount = 0

        for (event in events) {
            try {
                if (processEvent(event)) {
                    processedCount++
                }
            } catch (e: Exception) {
                logger.error("Failed to process event ${event.id}", e)
            }
        }

        logger.info("Successfully processed {}/{} events", processedCount, events.size)
        return processedCount
    }

    private fun processEvent(event: OutboxDAO): Boolean {
        val handler = handlerMap[event.eventType]

        if (handler == null) {
            logger.error("No handler found for event type: {}", event.eventType)
            event.markAsFailed("No handler found for event type: ${event.eventType}")
            outboxRepository.save(event)
            return false
        }

        event.markAsProcessing()
        outboxRepository.save(event)

        try {
            logger.debug("Processing event {} of type {} with handler {}", event.id, event.eventType, handler.javaClass.simpleName)
            handler.handle(event)

            event.markAsProcessed()
            outboxRepository.save(event)
            logger.debug("Event {} processed successfully", event.id)
            return true

        } catch (e: Exception) {
            logger.error("Handler failed to process event ${event.id}", e)
            event.markAsFailed("Handler failed: ${e.message}")
            outboxRepository.save(event)
            return false
        }
    }

    fun reprocessFailedEvents(): Int {
        val failedEvents = outboxRepository.findByStatus(OutboxStatus.FAILED)
            .filter { it.nextRetryAt == null || it.nextRetryAt!!.isBefore(LocalDateTime.now()) }

        if (failedEvents.isEmpty()) {
            return 0
        }

        logger.info("Reprocessing {} failed events", failedEvents.size)

        for (event in failedEvents) {
            event.resetForRetry()
        }

        outboxRepository.saveAll(failedEvents)
        return failedEvents.size
    }

    fun getDeadLetterCount(): Long = outboxRepository.countDeadLetterEvents()

    fun cleanupProcessedEvents(olderThanDays: Long = 7): Int {
        val cutoffDate = LocalDateTime.now().minusDays(olderThanDays)
        return outboxRepository.deleteProcessedEventsBefore(cutoffDate)
    }
}
