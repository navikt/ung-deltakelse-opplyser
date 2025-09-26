package no.nav.ung.deltakelseopplyser.outbox

import io.mockk.mockk
import io.mockk.verify
import io.mockk.every
import io.mockk.slot
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class OutboxProducerTest {

    private lateinit var outboxRepository: OutboxRepository
    private lateinit var outboxProducer: OutboxProducer

    @BeforeEach
    fun setup() {
        outboxRepository = mockk(relaxed = true)
        outboxProducer = OutboxProducer(outboxRepository)
    }

    @Test
    fun `should publish single event to outbox`() {
        val event = TestOutboxEvent("test-123", "Test data")
        val savedEventSlot = slot<OutboxDAO>()

        every { outboxRepository.save(capture(savedEventSlot)) } returns mockk()

        outboxProducer.publishEvent(event)

        verify { outboxRepository.save(any()) }
        val savedEvent = savedEventSlot.captured
        assertEquals("TEST_EVENT", savedEvent.eventType)
        assertEquals("test-123", savedEvent.aggregateId)
        assertEquals(OutboxStatus.PENDING, savedEvent.status)
    }

    @Test
    fun `should publish multiple events to outbox`() {
        val events = listOf(
            TestOutboxEvent("test-1", "Data 1"),
            TestOutboxEvent("test-2", "Data 2")
        )

        outboxProducer.publishEvents(events)

        verify { outboxRepository.saveAll(any<List<OutboxDAO>>()) }
    }

    @Test
    fun `should handle empty event list`() {
        outboxProducer.publishEvents(emptyList())

        verify(exactly = 0) { outboxRepository.saveAll(any<List<OutboxDAO>>()) }
    }

    private data class TestOutboxEvent(
        override val aggregateId: String,
        val data: String
    ) : OutboxEvent {
        override val eventType: String = "TEST_EVENT"
        override fun toJson(): String = """{"aggregateId":"$aggregateId","data":"$data"}"""
    }
}
