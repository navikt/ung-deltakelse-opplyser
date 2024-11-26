package no.nav.ung.deltakelseopplyser.register

import java.time.LocalDate
import java.util.*

data class DeltakerOpplysningerDTO(
    val opplysninger: List<DeltakelseOpplysningDTO>,
)

data class DeltakelseOpplysningDTO(
    val id: UUID? = null,
    @Deprecated("Bruk deltaker i stedet")
    val deltakerIdent: String,
    val deltaker: DeltakerDTO? = null,
    val harSøkt: Boolean,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate? = null,
) {

    fun deltaker(): DeltakerDTO {
        return deltaker ?: DeltakerDTO(deltakerIdent = deltakerIdent)
    }

    override fun toString(): String {
        return "DeltakerProgramOpplysningDTO(id=$id, fraOgMed=$fraOgMed, tilOgMed=$tilOgMed)"
    }
}
