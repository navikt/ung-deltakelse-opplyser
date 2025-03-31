package no.nav.ung.deltakelseopplyser.domene.register

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.*

interface UngdomsprogramDeltakelseRepository : JpaRepository<UngdomsprogramDeltakelseDAO, UUID> {
    fun findByDeltaker_IdIn(deltakerIds: List<UUID>): List<UngdomsprogramDeltakelseDAO>

    fun findByIdAndDeltaker_IdIn(id: UUID, deltakerIds: List<UUID>): UngdomsprogramDeltakelseDAO?

    @Query(
        value = """
        SELECT * FROM ungdomsprogram_deltakelse
        WHERE deltaker_id IN (:deltakterIder)
          AND lower(periode) = :periodeStartdato
        LIMIT 1
    """,
        nativeQuery = true
    )
    fun finnDeltakelseSomStarter(
        @Param("deltakterIder") deltakterIder: List<UUID>,
        @Param("periodeStartdato") periodeStartdato: LocalDate
    ): UngdomsprogramDeltakelseDAO?

    @Query(
        value = """
        SELECT u.* FROM ungdomsprogram_deltakelse u
        INNER JOIN oppgave o on u.id = o.deltakelse_id
        WHERE o.oppgave_referanse = :oppgaveReferanse
    """,
        nativeQuery = true
    )
    fun finnDeltakelseGittOppgaveReferanse(@Param("oppgaveReferanse") oppgaveReferanse: UUID): UngdomsprogramDeltakelseDAO?
}
