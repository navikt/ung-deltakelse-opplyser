package no.nav.ung.deltakelseopplyser.kontrakt.deltaker

import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveDTO
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
