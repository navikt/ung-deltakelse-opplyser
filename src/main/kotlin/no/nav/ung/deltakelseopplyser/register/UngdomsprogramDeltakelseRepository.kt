package no.nav.ung.deltakelseopplyser.register

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UngdomsprogramDeltakelseRepository: JpaRepository<UngdomsprogramDeltakelseDAO, UUID> {
    fun findByDeltakerIdent(deltakerId: String): List<UngdomsprogramDeltakelseDAO>
}
