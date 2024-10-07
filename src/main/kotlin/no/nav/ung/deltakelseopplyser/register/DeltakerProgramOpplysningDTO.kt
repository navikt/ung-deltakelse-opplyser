package no.nav.ung.deltakelseopplyser.register

import java.time.LocalDate
import java.util.*

data class DeltakerProgramOpplysningDTO(
    val id: UUID,
    val deltakerIdent: String,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate? = null,
)
