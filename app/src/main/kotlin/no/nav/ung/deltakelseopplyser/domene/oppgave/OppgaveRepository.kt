package no.nav.ung.deltakelseopplyser.domene.oppgave

import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveDAO
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface OppgaveRepository : JpaRepository<OppgaveDAO, UUID> {
    fun findByOppgaveReferanse(oppgaveReferanse: UUID): OppgaveDAO?

    fun findAllByDeltaker_DeltakerIdent(deltakerIdent: String): List<OppgaveDAO>

    @Query(
        value = """
                SELECT count(*) FROM oppgave o
                WHERE o.lukket_dato is not null
            """,
        nativeQuery = true
    )
    fun finnAntallLukkedeOppgaver(): Long

    @Query(
        value = """
                SELECT count(*) FROM oppgave o
                WHERE o.status = :status
            """,
        nativeQuery = true
    )
    fun finnAntallOppgaverMedStatus(status: String): Long

    @Query(
        value = """
                SELECT count(*) FROM oppgave o
                WHERE o.åpnet_dato is not null
            """,
        nativeQuery = true
    )
    fun finnAntallÅpnetOppgaver(): Long

    @Query(
        value = """
                SELECT count(*) FROM oppgave o
                WHERE o.oppgavetype = :oppgavetype
            """,
        nativeQuery = true
    )
    fun finnAntallOppgaverAvType(oppgavetype : String): Long
}
