package no.nav.ung.deltakelseopplyser.outbox

import OutboxStatus
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.*

@Entity(name = "outbox")
open class OutboxDAO(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "event_type", nullable = false)
    val eventType: String,

    @Column(name = "aggregate_id", nullable = false)
    val aggregateId: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    val payload: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "processed_at")
    var processedAt: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: OutboxStatus = OutboxStatus.PENDING,

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,

    @Column(name = "max_retry_count", nullable = false)
    val maxRetryCount: Int = 3,

    @Column(name = "error_message")
    var errorMessage: String? = null,

    @Column(name = "next_retry_at")
    var nextRetryAt: LocalDateTime? = null
) {
    fun markAsProcessed() {
        status = OutboxStatus.PROCESSED
        processedAt = LocalDateTime.now()
    }

    fun markAsFailed(error: String) {
        status = if (retryCount >= maxRetryCount) OutboxStatus.DEAD_LETTER else OutboxStatus.FAILED
        errorMessage = error
        retryCount++

        if (status == OutboxStatus.FAILED) {
            // Exponential backoff: 2^retry_count minutes
            nextRetryAt = LocalDateTime.now().plusMinutes((1 shl retryCount).toLong())
        }
    }

    fun markAsProcessing() {
        status = OutboxStatus.PROCESSING
    }

    fun resetForRetry() {
        status = OutboxStatus.PENDING
        nextRetryAt = null
    }
}
