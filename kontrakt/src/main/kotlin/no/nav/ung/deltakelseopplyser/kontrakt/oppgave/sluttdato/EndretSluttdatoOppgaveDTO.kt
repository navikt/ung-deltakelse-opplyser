package no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class EndretSluttdatoOppgaveDTO(
    @JsonProperty("deltakerIdent") val deltakerIdent: String,
    @JsonProperty("oppgaveReferanse") val oppgaveReferanse: UUID,
    @JsonProperty("frist") val frist: LocalDateTime,
    @JsonProperty("nySluttdato") val nySluttdato: LocalDate,
    @JsonProperty("forrigeSluttdato") val forrigeSluttdato: LocalDate? = null,
)