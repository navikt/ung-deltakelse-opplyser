package no.nav.ung.deltakelseopplyser.outbox

interface OutboxEvent {
    val eventType: String
    val aggregateId: String
    fun toJson(): String
}
