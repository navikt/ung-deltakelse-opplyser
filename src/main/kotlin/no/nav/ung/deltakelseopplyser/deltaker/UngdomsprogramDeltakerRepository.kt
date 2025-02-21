package no.nav.ung.deltakelseopplyser.deltaker

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UngdomsprogramDeltakerRepository: JpaRepository<DeltakerDAO, UUID> {
    fun findByDeltakerIdent(deltakerIdent: String): DeltakerDAO?
    fun findByDeltakerIdentIn(deltakerIdenter: List<String>): List<DeltakerDAO>
}
