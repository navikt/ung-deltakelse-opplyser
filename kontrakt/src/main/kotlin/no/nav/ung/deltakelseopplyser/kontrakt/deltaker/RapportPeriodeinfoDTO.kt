package no.nav.ung.deltakelseopplyser.kontrakt.deltaker

import java.math.BigDecimal
import java.time.LocalDate

data class RapportPeriodeinfoDTO(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val harRapportert: Boolean,
    val arbeidstakerOgFrilansInntekt: BigDecimal? = null,
    val inntektFraYtelse: BigDecimal? = null,
    val summertInntekt: BigDecimal? = arbeidstakerOgFrilansInntekt?.add(inntektFraYtelse ?: BigDecimal.ZERO) ?: null,
)
