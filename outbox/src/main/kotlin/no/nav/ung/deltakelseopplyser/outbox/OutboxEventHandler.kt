package no.nav.ung.deltakelseopplyser.outbox

interface OutboxEventHandler {
    val eventType: String
    fun handle(event: OutboxDAO)
}
