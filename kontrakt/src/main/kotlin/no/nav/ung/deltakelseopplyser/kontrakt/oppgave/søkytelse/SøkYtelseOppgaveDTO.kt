package no.nav.ung.deltakelseopplyser.kontrakt.oppgave.søkytelse

import com.fasterxml.jackson.annotation.JsonProperty

data class SøkYtelseOppgaveDTO (
    @JsonProperty("deltakerIdent") val deltakerIdent: String,
)