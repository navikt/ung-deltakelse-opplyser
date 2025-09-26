package no.nav.ung.deltakelseopplyser.outbox

import OutboxStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface OutboxRepository : JpaRepository<OutboxDAO, UUID> {

    @Query("""
        SELECT o FROM outbox o 
        WHERE o.status = 'PENDING' 
        OR (o.status = 'FAILED' AND (o.nextRetryAt IS NULL OR o.nextRetryAt <= :now))
        ORDER BY o.createdAt ASC
    """)
    fun findPendingEvents(@Param("now") now: LocalDateTime = LocalDateTime.now()): List<OutboxDAO>

    @Query("""
        SELECT o FROM outbox o 
        WHERE o.status = 'PENDING' 
        OR (o.status = 'FAILED' AND (o.nextRetryAt IS NULL OR o.nextRetryAt <= :now))
        ORDER BY o.createdAt ASC
        LIMIT :limit
    """)
    fun findPendingEventsWithLimit(@Param("now") now: LocalDateTime = LocalDateTime.now(), @Param("limit") limit: Int): List<OutboxDAO>

    @Query("SELECT o FROM outbox o WHERE o.eventType = :eventType ORDER BY o.createdAt DESC")
    fun findByEventType(@Param("eventType") eventType: String): List<OutboxDAO>

    @Query("SELECT o FROM outbox o WHERE o.aggregateId = :aggregateId ORDER BY o.createdAt DESC")
    fun findByAggregateId(@Param("aggregateId") aggregateId: String): List<OutboxDAO>

    @Query("SELECT o FROM outbox o WHERE o.status = :status ORDER BY o.createdAt DESC")
    fun findByStatus(@Param("status") status: OutboxStatus): List<OutboxDAO>

    @Modifying
    @Query("DELETE FROM outbox o WHERE o.status = 'PROCESSED' AND o.processedAt < :cutoffDate")
    fun deleteProcessedEventsBefore(@Param("cutoffDate") cutoffDate: LocalDateTime): Int

    @Query("SELECT COUNT(o) FROM outbox o WHERE o.status = 'DEAD_LETTER'")
    fun countDeadLetterEvents(): Long
}
