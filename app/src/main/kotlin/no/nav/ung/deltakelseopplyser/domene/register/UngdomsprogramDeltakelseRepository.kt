package no.nav.ung.deltakelseopplyser.domene.register

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.*

interface UngdomsprogramDeltakelseRepository : JpaRepository<DeltakelseDAO, UUID> {
    fun findByDeltaker_IdIn(deltakerIds: List<UUID>): List<DeltakelseDAO>

    fun findByIdAndDeltaker_IdIn(id: UUID, deltakerIds: List<UUID>): DeltakelseDAO?

}
