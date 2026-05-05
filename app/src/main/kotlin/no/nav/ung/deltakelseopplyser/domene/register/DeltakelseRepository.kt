package no.nav.ung.deltakelseopplyser.domene.register

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.*

interface DeltakelseRepository : JpaRepository<DeltakelseDAO, UUID> {
    fun findByDeltaker_IdIn(deltakerIder: List<UUID>): List<DeltakelseDAO>
    fun findByDeltaker_IdInAndErSlettet(deltakerIder: List<UUID>, erSlettet: Boolean): List<DeltakelseDAO>

    /**
     * Finner aktive deltakelser der maksdato er passert (<=iDag)
     * og sluttdato (upper bound) ikke er satt, og deltakelsen ikke er slettet.
     */
    @Query("""
        SELECT d FROM ungdomsprogram_deltakelse d
        WHERE d.erSlettet = false
        AND d.maksDato IS NOT NULL
        AND d.maksDato <= :iDag
        AND upper(d.periode) IS NULL
    """)
    fun findAktiveDeltakelserMedMaksdatoPassert(@Param("iDag") iDag: LocalDate): List<DeltakelseDAO>
}
