package no.nav.ung.deltakelseopplyser.kontrakt.oppgave.inntektsrapportering

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class InntektsrapporteringOppgaveDTO(
    @JsonProperty("deltakerIdent") val deltakerIdent: String,
    @JsonProperty("referanse") val referanse: UUID,
    @JsonProperty("frist") val frist: LocalDateTime,
    @JsonProperty("fomDato") val fomDato: LocalDate,
    @JsonProperty("tomDato") val tomDato: LocalDate,
    @JsonProperty("gjelderDelerAvMåned") val gjelderDelerAvMåned: Boolean
)
