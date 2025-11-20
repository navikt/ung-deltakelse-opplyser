package no.nav.ung.deltakelseopplyser.domene.register

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface DeltakelseRepository : JpaRepository<DeltakelseDAO, UUID> {
    fun findByDeltaker_IdIn(deltakerIds: List<UUID>): List<DeltakelseDAO>
    fun findByDeltaker_IdAndEr_slettet(deltakerIds: List<UUID>, erSlettet: Boolean): List<DeltakelseDAO>

    fun findByIdAndDeltaker_IdIn(id: UUID, deltakerIds: List<UUID>): DeltakelseDAO?

}
