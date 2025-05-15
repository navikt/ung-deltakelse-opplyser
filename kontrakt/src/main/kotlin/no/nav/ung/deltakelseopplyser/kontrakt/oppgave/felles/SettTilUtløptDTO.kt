package no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class SettTilUtløptDTO(
    @JsonProperty("deltakerIdent") val deltakerIdent: String,
    @JsonProperty("oppgavetype") val oppgavetype: Oppgavetype,
    @JsonProperty("fomDato") val fomDato: LocalDate,
    @JsonProperty("tomDato") val tomDato: LocalDate
    )