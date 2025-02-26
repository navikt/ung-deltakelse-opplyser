package no.nav.ung.deltakelseopplyser.register

import no.nav.ung.deltakelseopplyser.oppgave.OppgaveDTO
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

data class DeltakelsePeriodInfo(
    val id: UUID,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate? = null,
    val harSøkt: Boolean,
    val oppgaver: List<OppgaveDTO>,
    val rapporteringsPerioder: List<RapportPeriodeinfoDTO> = emptyList(),
)

data class RapportPeriodeinfoDTO(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val harRapportert: Boolean,
    val inntekt: BigDecimal? = null,
)
