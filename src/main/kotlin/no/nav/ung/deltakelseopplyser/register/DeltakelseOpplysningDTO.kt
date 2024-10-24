package no.nav.ung.deltakelseopplyser.register

import java.time.LocalDate
import java.util.*

data class DeltakerOpplysningerDTO(
    val opplysninger: List<DeltakelseOpplysningDTO>,
)

data class DeltakelseOpplysningDTO(
    val id: UUID? = null,
    val deltakerIdent: String,
    val harSøkt: Boolean,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate? = null,
) {

    override fun toString(): String {
        return "DeltakerProgramOpplysningDTO(id=$id, deltakerIdentSatt='${deltakerIdent.isNotBlank()}', fraOgMed=$fraOgMed, tilOgMed=$tilOgMed)"
    }
}
