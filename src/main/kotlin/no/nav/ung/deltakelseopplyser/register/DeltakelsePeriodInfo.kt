package no.nav.ung.deltakelseopplyser.register

import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

data class DeltakelsePeriodInfo(
    val id: UUID,
    val programperiodeFraOgMed: LocalDate,
    val programperiodeTilOgMed: LocalDate? = null,
    val harSøkt: Boolean,
    val rapporteringsPerioder: List<RapportPeriodeinfoDTO> = emptyList(),
)

data class RapportPeriodeinfoDTO(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val harRapportert: Boolean,
    val inntekt: BigDecimal? = null,
)
