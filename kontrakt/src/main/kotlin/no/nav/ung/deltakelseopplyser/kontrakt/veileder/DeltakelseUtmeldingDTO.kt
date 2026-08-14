package no.nav.ung.deltakelseopplyser.kontrakt.veileder

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.ung.deltakelseopplyser.kontrakt.register.Avslutningsårsak
import java.time.LocalDate

data class DeltakelseUtmeldingDTO(
    @JsonProperty("utmeldingsdato") val utmeldingsdato: LocalDate,
    @JsonProperty("avslutningsårsak") val avslutningsårsak: Avslutningsårsak? = null,
) {

    override fun toString(): String {
        return "DeltakelseUtmeldingDTO(utmeldingsdato=$utmeldingsdato, avslutningsårsak=$avslutningsårsak)"
    }
}
