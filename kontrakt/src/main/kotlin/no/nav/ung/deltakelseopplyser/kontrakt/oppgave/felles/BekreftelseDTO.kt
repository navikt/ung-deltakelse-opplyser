package no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles

import com.fasterxml.jackson.annotation.JsonProperty

class BekreftelseDTO(
    @JsonProperty("harGodtattEndringen") val harGodtattEndringen: Boolean,
    @JsonProperty("uttalelseFraBruker") val uttalelseFraBruker: String? = null,
)
