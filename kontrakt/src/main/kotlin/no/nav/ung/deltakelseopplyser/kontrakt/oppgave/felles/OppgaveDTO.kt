package no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles

import java.time.ZonedDateTime
import java.util.*

data class OppgaveDTO(
    val id: UUID,
    val eksternReferanse: UUID,
    val oppgavetype: Oppgavetype,
    val oppgavetypeData: OppgavetypeDataDTO,
    val status: OppgaveStatus,
    val opprettetDato: ZonedDateTime,
    val løstDato: ZonedDateTime?,
)
