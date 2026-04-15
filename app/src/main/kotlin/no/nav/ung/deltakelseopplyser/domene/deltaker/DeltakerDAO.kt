package no.nav.ung.deltakelseopplyser.domene.deltaker

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import no.nav.ung.deltakelseopplyser.domene.minside.mikrofrontend.MinSideMicrofrontendStatusDAO
import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseDAO
import java.util.*

@Entity(name = "deltaker")
class DeltakerDAO(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "deltaker_ident", unique = true, nullable = false)
    val deltakerIdent: String,

    @OneToMany(
        mappedBy = "deltaker",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    ) // Refererer til UngdomsprogramDeltakelseDAO
    val deltakelseList: List<DeltakelseDAO> = emptyList(),


    @OneToOne(mappedBy = "deltaker", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var minSideMicrofrontendStatusDAO: MinSideMicrofrontendStatusDAO? = null,
)
