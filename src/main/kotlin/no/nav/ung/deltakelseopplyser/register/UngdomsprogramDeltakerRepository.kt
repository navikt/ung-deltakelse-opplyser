package no.nav.ung.deltakelseopplyser.register

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UngdomsprogramDeltakerRepository: JpaRepository<DeltakerDAO, UUID> {
    fun existsByDeltakerIdent(deltakerIdent: String): Boolean
    fun findByDeltakerIdentIn(deltakerIdenter: List<String>): List<DeltakerDAO>
}
