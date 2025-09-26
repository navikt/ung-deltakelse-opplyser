package no.nav.ung.deltakelseopplyser.outbox

import io.mockk.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDateTime
import java.util.*

class OutboxConsumerTest {

    private lateinit var outboxRepository: OutboxRepository
    private lateinit var eventHandler: OutboxEventHandler
    private lateinit var outboxConsumer: OutboxConsumer

    @BeforeEach
    fun setup() {
        outboxRepository = mockk()
        eventHandler = mockk()
        every { eventHandler.eventType } returns "TEST_EVENT"
        outboxConsumer = OutboxConsumer(outboxRepository, listOf(eventHandler))
    }

    @Test
    fun `should process pending events successfully`() {
        val event = OutboxDAO(
            id = UUID.randomUUID(),
            eventType = "TEST_EVENT",
            aggregateId = "test-123",
            payload = """{"test": "data"}"""
        )

        every { outboxRepository.findPendingEventsWithLimit(any(), any()) } returns listOf(event)
        every { eventHandler.handle(any()) } just runs
        every { outboxRepository.save(any()) } returns event

        val processedCount = outboxConsumer.processEvents(10)

        assertEquals(1, processedCount)
        verify { eventHandler.handle(event) }
        verify(exactly = 2) { outboxRepository.save(any()) } // Once for PROCESSING, once for PROCESSED
        assertEquals(OutboxStatus.PROCESSED, event.status)
        assertNotNull(event.processedAt)
    }

    @Test
    fun `should handle event processing failure`() {
        val event = OutboxDAO(
            id = UUID.randomUUID(),
            eventType = "TEST_EVENT",
            aggregateId = "test-123",
            payload = """{"test": "data"}"""
        )

        every { outboxRepository.findPendingEventsWithLimit(any(), any()) } returns listOf(event)
        every { eventHandler.handle(any()) } throws RuntimeException("Processing failed")
        every { outboxRepository.save(any()) } returns event

        val processedCount = outboxConsumer.processEvents(10)

        assertEquals(0, processedCount)
        verify(exactly = 2) { outboxRepository.save(any()) } // Once for PROCESSING, once for FAILED
        assertEquals(OutboxStatus.FAILED, event.status)
        assertEquals("Handler failed: Processing failed", event.errorMessage)
    }

    @Test
    fun `should handle missing event handler`() {
        val event = OutboxDAO(
            id = UUID.randomUUID(),
            eventType = "UNKNOWN_EVENT",
            aggregateId = "test-123",
            payload = """{"test": "data"}"""
        )

        every { outboxRepository.findPendingEventsWithLimit(any(), any()) } returns listOf(event)
        every { outboxRepository.save(any()) } returns event

        val processedCount = outboxConsumer.processEvents(10)

        assertEquals(0, processedCount)
        verify { outboxRepository.save(match {
            it.status == OutboxStatus.FAILED &&
            it.errorMessage?.contains("No handler found") == true
        }) }
    }

    @Test
    fun `should reprocess failed events`() {
        val failedEvent = OutboxDAO(
            id = UUID.randomUUID(),
            eventType = "TEST_EVENT",
            aggregateId = "test-123",
            payload = """{"test": "data"}"""
        )
        failedEvent.status = OutboxStatus.FAILED
        failedEvent.nextRetryAt = LocalDateTime.now().minusMinutes(1)

        every { outboxRepository.findByStatus(OutboxStatus.FAILED) } returns listOf(failedEvent)
        every { outboxRepository.saveAll(any<List<OutboxDAO>>()) } returns listOf(failedEvent)

        val retriedCount = outboxConsumer.reprocessFailedEvents()

        assertEquals(1, retriedCount)
        verify { outboxRepository.saveAll(match<List<OutboxDAO>> {
            it.size == 1 && it[0].status == OutboxStatus.PENDING
        }) }
    }

    @Test
    fun `should cleanup old processed events`() {
        every { outboxRepository.deleteProcessedEventsBefore(any()) } returns 5

        val cleanedCount = outboxConsumer.cleanupProcessedEvents(7)

        assertEquals(5, cleanedCount)
        verify { outboxRepository.deleteProcessedEventsBefore(any()) }
    }
}
