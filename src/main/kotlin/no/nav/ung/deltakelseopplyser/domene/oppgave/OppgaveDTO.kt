package no.nav.ung.deltakelseopplyser.domene.oppgave

import java.time.ZonedDateTime
import java.util.*

data class OppgaveDTO(
    val id: UUID,
    val oppgavetype: Oppgavetype,
    val oppgavetypeData: OppgavetypeDataDTO,
    val status: OppgaveStatus,
    val opprettetDato: ZonedDateTime,
    val løstDato: ZonedDateTime?,
) {
    companion object {
        fun OppgaveDAO.tilDTO() = OppgaveDTO(
            id = id,
            oppgavetype = oppgavetype,
            oppgavetypeData = oppgavetypeDataDAO.tilDTO(),
            status = status,
            opprettetDato = opprettetDato,
            løstDato = løstDato
        )
    }
}
