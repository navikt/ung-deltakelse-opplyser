package no.nav.ung.deltakelseopplyser.kontrakt.veileder

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class DeltakelseUtmeldingDTO(
    @JsonProperty("utmeldingsdato") val utmeldingsdato: LocalDate,
) {

    override fun toString(): String {
        return "DeltakelseUtmeldingDTO(utmeldingsdato=$utmeldingsdato)"
    }
}
