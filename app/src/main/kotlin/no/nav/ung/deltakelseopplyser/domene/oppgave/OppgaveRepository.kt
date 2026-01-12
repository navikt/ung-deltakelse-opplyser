package no.nav.ung.deltakelseopplyser.domene.oppgave

import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveDAO
import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseDAO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveStatus
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface OppgaveRepository : JpaRepository<OppgaveDAO, UUID> {
    fun findByOppgaveReferanse(oppgaveReferanse: UUID): OppgaveDAO?
    fun findAllByOppgavetypeAndStatus(oppgavetype: Oppgavetype, status: OppgaveStatus): List<OppgaveDAO>
}
