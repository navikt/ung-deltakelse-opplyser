package no.nav.ung.deltakelseopplyser.register

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UngdomsprogramDeltakelseRepository : JpaRepository<UngdomsprogramDeltakelseDAO, UUID> {
    fun findByDeltaker_IdIn(deltakerIds: List<UUID>): List<UngdomsprogramDeltakelseDAO>
}
