package no.nav.ung.deltakelseopplyser.kontrakt.deltaker

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDate

data class RapportPeriodeinfoDTO(
    @JsonProperty("fraOgMed") val fraOgMed: LocalDate,
    @JsonProperty("tilOgMed") val tilOgMed: LocalDate,
    @JsonProperty("harRapportert") val harRapportert: Boolean,
    @JsonProperty("arbeidstakerOgFrilansInntekt") val arbeidstakerOgFrilansInntekt: BigDecimal? = null,
    @JsonProperty("inntektFraYtelse") val inntektFraYtelse: BigDecimal? = null,
    @JsonProperty("summertInntekt") val summertInntekt: BigDecimal? = arbeidstakerOgFrilansInntekt?.add(inntektFraYtelse ?: BigDecimal.ZERO) ?: null,
)
