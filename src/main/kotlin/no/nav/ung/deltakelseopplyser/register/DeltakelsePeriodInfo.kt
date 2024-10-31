package no.nav.ung.deltakelseopplyser.register

import java.time.LocalDate

data class DeltakelsePeriodInfo(
    val deltakerIdent: String? = null,
    val programPeriodeFraOgMed: LocalDate,
    val programperiodeTilOgMed: LocalDate? = null,
    val harSøkt: Boolean,
    val rapporteringsPerioder: List<RapportPeriodeinfoDTO> = emptyList(),
)

data class RapportPeriodeinfoDTO(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val harSøkt: Boolean,
    val inntekt: Double? = null,
)
