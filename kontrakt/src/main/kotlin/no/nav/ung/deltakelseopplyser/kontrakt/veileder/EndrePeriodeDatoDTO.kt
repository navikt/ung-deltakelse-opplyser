package no.nav.ung.deltakelseopplyser.kontrakt.veileder

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class EndrePeriodeDatoDTO(
    @JsonProperty("dato") val dato: LocalDate,
    @JsonProperty("veilederRef") val veilederRef: String,
    @JsonProperty("meldingFraVeileder") val meldingFraVeileder: String?,
)
