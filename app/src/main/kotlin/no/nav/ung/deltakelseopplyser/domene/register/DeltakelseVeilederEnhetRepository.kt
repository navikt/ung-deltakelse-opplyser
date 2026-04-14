package no.nav.ung.deltakelseopplyser.domene.register

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface DeltakelseVeilederEnhetRepository : JpaRepository<DeltakelseVeilederEnhetDAO, UUID> {
    fun findAllByDeltakelseIdIn(deltakelseIder: List<UUID>): List<DeltakelseVeilederEnhetDAO>
    fun findByDeltakelseId(deltakelseId: UUID): DeltakelseVeilederEnhetDAO?
}

