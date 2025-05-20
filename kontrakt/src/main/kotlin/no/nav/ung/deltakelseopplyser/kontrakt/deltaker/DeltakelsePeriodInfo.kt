package no.nav.ung.deltakelseopplyser.kontrakt.deltaker

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveDTO
import java.time.LocalDate
import java.util.*

data class DeltakelsePeriodInfo(
    @JsonProperty("id") val id: UUID,
    @JsonProperty("fraOgMed") val fraOgMed: LocalDate,
    @JsonProperty("tilOgMed") val tilOgMed: LocalDate? = null,
    @JsonProperty("harSøkt") val harSøkt: Boolean,
    @JsonProperty("oppgaver") val oppgaver: List<OppgaveDTO>,
)
