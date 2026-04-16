package no.nav.ung.deltakelseopplyser.domene.register

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface DeltakelseVeilederEnhetRepository : JpaRepository<DeltakelseVeilederEnhetDAO, UUID> {
    fun findAllByDeltakelseIdIn(deltakelseIder: List<UUID>): List<DeltakelseVeilederEnhetDAO>
    fun findByDeltakelseId(deltakelseId: UUID): DeltakelseVeilederEnhetDAO?

    @org.springframework.data.jpa.repository.Query(
        "SELECT d.enhetId AS enhetId, d.enhetNavn AS enhetNavn, COUNT(d) AS antall " +
                "FROM DeltakelseVeilederEnhetDAO d GROUP BY d.enhetId, d.enhetNavn"
    )
    fun hentEnhetPopularitet(): List<EnhetPopularitetProjection>
}

interface EnhetPopularitetProjection {
    val enhetId: String
    val enhetNavn: String
    val antall: Long
}

