package no.nav.ung.deltakelseopplyser.register

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UngdomsprogramRepository: JpaRepository<UngdomsprogramDAO, UUID> {
    fun findByDeltakerIdent(deltakerId: String): List<UngdomsprogramDAO>
}
