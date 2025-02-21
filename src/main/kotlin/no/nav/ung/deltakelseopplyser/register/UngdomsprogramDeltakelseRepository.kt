package no.nav.ung.deltakelseopplyser.register

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.*

interface UngdomsprogramDeltakelseRepository : JpaRepository<UngdomsprogramDeltakelseDAO, UUID> {
    fun findByDeltaker_IdIn(deltakerIds: List<UUID>): List<UngdomsprogramDeltakelseDAO>

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
}
