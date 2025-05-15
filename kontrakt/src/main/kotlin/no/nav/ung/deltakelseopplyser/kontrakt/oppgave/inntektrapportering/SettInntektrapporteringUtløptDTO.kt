package no.nav.ung.deltakelseopplyser.kontrakt.oppgave.inntektrapportering

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class SettInntektrapporteringUtløptDTO(
    @JsonProperty("deltakerIdent") val deltakerIdent: String,
    @JsonProperty("fomDato") val fomDato: LocalDate,
    @JsonProperty("tomDato") val tomDato: LocalDate
    )