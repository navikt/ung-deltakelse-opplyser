package no.nav.ung.deltakelseopplyser.domene.register

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface DeltakelseRepository : JpaRepository<DeltakelseDAO, UUID> {
    fun findByDeltaker_IdIn(deltakerIds: List<UUID>): List<DeltakelseDAO>
    fun findByDeltaker_IdInAndErSlettet(deltakerIds: List<UUID>, erSlettet: Boolean): List<DeltakelseDAO>

    fun findByIdAndDeltaker_IdIn(id: UUID, deltakerIds: List<UUID>): DeltakelseDAO?

    @Query(
        """
        SELECT d FROM ungdomsprogram_deltakelse d
        JOIN FETCH d.deltaker
        WHERE d.erSlettet = false
        """
    )
    fun findAlleIkkeSlettet(): List<DeltakelseDAO>
}
