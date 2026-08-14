package no.nav.ung.deltakelseopplyser.kontrakt.veileder

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.ung.deltakelseopplyser.kontrakt.register.Avslutningsårsak
import java.time.LocalDate

data class DeltakelseUtmeldingDTO(
    @JsonProperty("utmeldingsdato") val utmeldingsdato: LocalDate,
    // TODO: Fjern nullable når dette er lansert og frontend har migrert. Den er nullable for bakoverkompatibilitet.
    @JsonProperty("avslutningsårsak") val avslutningsårsak: Avslutningsårsak? = null,
) {

    override fun toString(): String {
        return "DeltakelseUtmeldingDTO(utmeldingsdato=$utmeldingsdato, avslutningsårsak=$avslutningsårsak)"
    }
}
