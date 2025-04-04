package no.nav.ung.deltakelseopplyser.kontrakt.oppgave.periodeendring

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class EndretPeriodeOppgaveDTO(
    @JsonProperty("deltakerIdent") val deltakerIdent: String,
    @JsonProperty("oppgaveReferanse") val oppgaveReferanse: UUID,
    @JsonProperty("frist") val frist: LocalDateTime,
    @JsonProperty("programperiodeDato") val programperiodeDato: LocalDate
)
