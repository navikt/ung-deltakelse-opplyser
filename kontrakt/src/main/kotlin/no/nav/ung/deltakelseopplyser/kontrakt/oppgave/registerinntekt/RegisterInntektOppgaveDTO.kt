package no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class RegisterInntektOppgaveDTO(
    @JsonProperty("deltakerIdent") val deltakerIdent: String,
    @JsonProperty("referanse") val referanse: UUID,
    @JsonProperty("frist") val frist: LocalDateTime,
    @JsonProperty("fomDato") val fomDato: LocalDate,
    @JsonProperty("tomDato") val tomDato: LocalDate,
    @JsonProperty("registerInntekter") val registerInntekter: RegisterInntektDTO,
    @JsonProperty("gjelderDelerAvMåned") val gjelderDelerAvMåned: Boolean,
    )

data class RegisterInntektDTO(
    @JsonProperty("registerinntekterForArbeidOgFrilans") val registerinntekterForArbeidOgFrilans: List<RegisterInntektArbeidOgFrilansDTO>? = listOf(),
    @JsonProperty("registerinntekterForYtelse") val registerinntekterForYtelse: List<RegisterInntektYtelseDTO>? = listOf(),
)

data class RegisterInntektYtelseDTO (
    @JsonProperty("beløp") val beløp: Int,
    @JsonProperty("ytelseType") val ytelseType: YtelseType,
)

data class RegisterInntektArbeidOgFrilansDTO (
    @JsonProperty("beløp") val beløp: Int,
    @JsonProperty("arbeidsgiverIdent") val arbeidsgiverIdent: String,
)

enum class YtelseType {
    SYKEPENGER,
    OMSORGSPENGER,
    PLEIEPENGER_SYKT_BARN,
    PLEIEPENGER_LIVETS_SLUTTFASE,
    OPPLAERINGSPENGER
}

