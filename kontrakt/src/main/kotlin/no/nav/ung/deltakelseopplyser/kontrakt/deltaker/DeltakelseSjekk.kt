package no.nav.ung.deltakelseopplyser.kontrakt.deltaker

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class DeltakelseSjekk(
    @JsonProperty("erDeltaker") val erDeltaker: Boolean,
    @JsonProperty("fraOgMed") val fraOgMed: LocalDate? = null,
    @JsonProperty("tilOgMed") val tilOgMed: LocalDate? = null,
)
