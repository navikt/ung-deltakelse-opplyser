package no.nav.ung.deltakelseopplyser.domene.oppgave

import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveStatus
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.Oppgavetype
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
) {
    companion object {
        fun OppgaveDAO.tilDTO() = OppgaveDTO(
            id = id,
            eksternReferanse = eksternReferanse,
            oppgavetype = oppgavetype,
            oppgavetypeData = oppgavetypeDataDAO.tilDTO(),
            status = status,
            opprettetDato = opprettetDato,
            løstDato = løstDato
        )
    }
}
