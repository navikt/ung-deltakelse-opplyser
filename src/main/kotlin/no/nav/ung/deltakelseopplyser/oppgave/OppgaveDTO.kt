package no.nav.ung.deltakelseopplyser.oppgave

import java.time.ZonedDateTime
import java.util.*

data class OppgaveDTO(
    val id: UUID,
    val oppgavetype: Oppgavetype,
    val oppgavetypeDataDTO: OppgavetypeDataDTO,
    val status: OppgaveStatus,
    val opprettetDato: ZonedDateTime,
    val løstDato: ZonedDateTime?,
) {
    companion object {
        fun OppgaveDAO.tilDTO() = OppgaveDTO(
            id = id,
            oppgavetype = oppgavetype,
            oppgavetypeDataDTO = oppgavetypeData.tilDTO(),
            status = status,
            opprettetDato = opprettetDato,
            løstDato = løstDato
        )
    }
}
