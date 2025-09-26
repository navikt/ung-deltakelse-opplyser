package no.nav.ung.deltakelseopplyser.outbox

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["outbox.scheduler.enabled"], havingValue = "true", matchIfMissing = true)
class OutboxScheduler(
    private val outboxConsumer: OutboxConsumer
) {
    private val logger = LoggerFactory.getLogger(OutboxScheduler::class.java)

    @Scheduled(fixedDelayString = "\${outbox.scheduler.process-interval:5000}") // Default 5 seconds
    fun processOutboxEvents() {
        try {
            val processedCount = outboxConsumer.processEvents()
            if (processedCount > 0) {
                logger.debug("Processed {} outbox events", processedCount)
            }
        } catch (e: Exception) {
            logger.error("Error processing outbox events", e)
        }
    }

    @Scheduled(fixedDelayString = "\${outbox.scheduler.retry-interval:60000}") // Default 1 minute
    fun retryFailedEvents() {
        try {
            val retriedCount = outboxConsumer.reprocessFailedEvents()
            if (retriedCount > 0) {
                logger.info("Reset {} failed events for retry", retriedCount)
            }
        } catch (e: Exception) {
            logger.error("Error retrying failed outbox events", e)
        }
    }

    @Scheduled(fixedDelayString = "\${outbox.scheduler.cleanup-interval:3600000}") // Default 1 hour
    fun cleanupProcessedEvents() {
        try {
            val cleanedCount = outboxConsumer.cleanupProcessedEvents()
            if (cleanedCount > 0) {
                logger.info("Cleaned up {} processed outbox events", cleanedCount)
            }
        } catch (e: Exception) {
            logger.error("Error cleaning up processed outbox events", e)
        }
    }

    @Scheduled(fixedDelayString = "\${outbox.scheduler.dead-letter-check-interval:300000}") // Default 5 minutes
    fun checkDeadLetterEvents() {
        try {
            val deadLetterCount = outboxConsumer.getDeadLetterCount()
            if (deadLetterCount > 0) {
                logger.warn("Found {} dead letter events that require manual intervention", deadLetterCount)
            }
        } catch (e: Exception) {
            logger.error("Error checking dead letter events", e)
        }
    }
}
