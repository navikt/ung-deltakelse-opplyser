package no.nav.ung.deltakelseopplyser.domene.oppgave

import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveDAO
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface OppgaveRepository : JpaRepository<OppgaveDAO, UUID> {
    fun findByOppgaveReferanse(oppgaveReferanse: UUID): OppgaveDAO?

    fun findAllByDeltaker_DeltakerIdent(deltakerIdent: String): List<OppgaveDAO>
}
