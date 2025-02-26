package no.nav.ung.deltakelseopplyser.domene.deltaker

import java.util.UUID
import jakarta.persistence.*
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramDeltakelseDAO

@Entity(name = "deltaker")
class DeltakerDAO(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "deltaker_ident", unique = true, nullable = false)
    val deltakerIdent: String,

    @OneToMany(mappedBy = "deltaker") // Refererer til UngdomsprogramDeltakelseDAO
    val deltakelseList: List<UngdomsprogramDeltakelseDAO> = emptyList(),
)
