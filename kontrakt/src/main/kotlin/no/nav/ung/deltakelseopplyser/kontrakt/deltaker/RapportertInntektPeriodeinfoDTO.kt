package no.nav.ung.deltakelseopplyser.kontrakt.deltaker

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDate

data class RapportertInntektPeriodeinfoDTO(
    @JsonProperty("fraOgMed") val fraOgMed: LocalDate,
    @JsonProperty("tilOgMed") val tilOgMed: LocalDate,
    @JsonProperty("arbeidstakerOgFrilansInntekt") val arbeidstakerOgFrilansInntekt: BigDecimal? = null
)
