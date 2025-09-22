package no.nav.ung.deltakelseopplyser.domene.deltaker

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface DeltakerRepository : JpaRepository<DeltakerDAO, UUID> {
    @Query(
        value = """
                SELECT d.* FROM deltaker d
                WHERE d.deltaker_ident IN (:deltakerIdenter)
            """,
        nativeQuery = true
    )
    fun finnDeltakerGittIdenter(@Param("deltakerIdenter") deltakerIdenter: List<String>): List<DeltakerDAO>

    @Query(
        value = """
        SELECT d.* FROM deltaker d
        INNER JOIN oppgave o on d.id = o.deltaker_id
        WHERE o.oppgave_referanse = :oppgaveReferanse
    """,
        nativeQuery = true
    )
    fun finnDeltakerGittOppgaveReferanse(@Param("oppgaveReferanse") oppgaveReferanse: UUID): DeltakerDAO?
}
