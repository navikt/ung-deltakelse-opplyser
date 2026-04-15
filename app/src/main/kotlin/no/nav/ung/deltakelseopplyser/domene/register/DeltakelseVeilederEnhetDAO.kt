package no.nav.ung.deltakelseopplyser.domene.register

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.*

@Entity
@Table(name = "deltakelse_veileder_enhet")
class DeltakelseVeilederEnhetDAO(
    @Id
    @Column(name = "deltakelse_id")
    val deltakelseId: UUID,

    @Column(name = "nav_ident", nullable = false)
    val navIdent: String,

    @Column(name = "enhet_id", nullable = false)
    var enhetId: String,

    @Column(name = "enhet_navn", nullable = false)
    var enhetNavn: String,

    @Column(name = "opprettet_tidspunkt", nullable = false, updatable = false)
    val opprettetTidspunkt: Instant = Instant.now(),
)

