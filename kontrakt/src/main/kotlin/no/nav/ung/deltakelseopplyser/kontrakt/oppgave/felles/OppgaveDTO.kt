package no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.ZonedDateTime
import java.util.*

data class OppgaveDTO(
    @JsonProperty("oppgaveReferanse") val oppgaveReferanse: UUID,
    @JsonProperty("oppgavetype") val oppgavetype: Oppgavetype,
    @JsonProperty("oppgavetypeData") val oppgavetypeData: OppgavetypeDataDTO,
    @JsonProperty("status") val status: OppgaveStatus,
    @JsonProperty("opprettetDato") val opprettetDato: ZonedDateTime,
    @JsonProperty("løstDato") val løstDato: ZonedDateTime?,
)
