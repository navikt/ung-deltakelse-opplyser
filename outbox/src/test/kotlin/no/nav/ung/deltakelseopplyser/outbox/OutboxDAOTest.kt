package no.nav.ung.deltakelseopplyser.outbox

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime
import java.util.*

class OutboxDAOTest {

    @Test
    fun `should create outbox entry with default values`() {
        val outbox = OutboxDAO(
            eventType = "TEST_EVENT",
            aggregateId = "test-123",
            payload = """{"test": "data"}"""
        )

        assertEquals("TEST_EVENT", outbox.eventType)
        assertEquals("test-123", outbox.aggregateId)
        assertEquals("""{"test": "data"}""", outbox.payload)
        assertEquals(OutboxStatus.PENDING, outbox.status)
        assertEquals(0, outbox.retryCount)
        assertEquals(3, outbox.maxRetryCount)
        assertNull(outbox.processedAt)
        assertNull(outbox.errorMessage)
        assertNull(outbox.nextRetryAt)
        assertNotNull(outbox.createdAt)
    }

    @Test
    fun `should mark as processed`() {
        val outbox = OutboxDAO(
            eventType = "TEST_EVENT",
            aggregateId = "test-123",
            payload = """{"test": "data"}"""
        )

        outbox.markAsProcessed()

        assertEquals(OutboxStatus.PROCESSED, outbox.status)
        assertNotNull(outbox.processedAt)
    }

    @Test
    fun `should mark as failed and increment retry count`() {
        val outbox = OutboxDAO(
            eventType = "TEST_EVENT",
            aggregateId = "test-123",
            payload = """{"test": "data"}"""
        )

        outbox.markAsFailed("Test error")

        assertEquals(OutboxStatus.FAILED, outbox.status)
        assertEquals(1, outbox.retryCount)
        assertEquals("Test error", outbox.errorMessage)
        assertNotNull(outbox.nextRetryAt)
    }

    @Test
    fun `should mark as dead letter when max retries exceeded`() {
        val outbox = OutboxDAO(
            eventType = "TEST_EVENT",
            aggregateId = "test-123",
            payload = """{"test": "data"}""",
            maxRetryCount = 1
        )

        outbox.markAsFailed("First failure")
        assertEquals(OutboxStatus.FAILED, outbox.status)

        outbox.markAsFailed("Second failure")
        assertEquals(OutboxStatus.DEAD_LETTER, outbox.status)
        assertEquals(2, outbox.retryCount)
    }

    @Test
    fun `should mark as processing`() {
        val outbox = OutboxDAO(
            eventType = "TEST_EVENT",
            aggregateId = "test-123",
            payload = """{"test": "data"}"""
        )

        outbox.markAsProcessing()

        assertEquals(OutboxStatus.PROCESSING, outbox.status)
    }

    @Test
    fun `should reset for retry`() {
        val outbox = OutboxDAO(
            eventType = "TEST_EVENT",
            aggregateId = "test-123",
            payload = """{"test": "data"}"""
        )

        outbox.markAsFailed("Test error")
        outbox.resetForRetry()

        assertEquals(OutboxStatus.PENDING, outbox.status)
        assertNull(outbox.nextRetryAt)
    }
}
