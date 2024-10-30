package no.nav.ung.deltakelseopplyser.register

import java.time.LocalDate
import java.util.UUID

data class InntektIPeriodeDTO(
    val deltakelseId: String,
    val deltakerIdent: String,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val inntekt: Number? = null
)
