package no.nav.ung.deltakelseopplyser.kontrakt.deltaker

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveDTO
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

data class DeltakelsePeriodInfo(
    @JsonProperty("id") val id: UUID,
    @JsonProperty("fraOgMed") val fraOgMed: LocalDate,
    @JsonProperty("tilOgMed") val tilOgMed: LocalDate? = null,
    @JsonProperty("søktTidspunkt") val søktTidspunkt: ZonedDateTime? = null,
    @JsonProperty("oppgaver") val oppgaver: List<OppgaveDTO>,
)
