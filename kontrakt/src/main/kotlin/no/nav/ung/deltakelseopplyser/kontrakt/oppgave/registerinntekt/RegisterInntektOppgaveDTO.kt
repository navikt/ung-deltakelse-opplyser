package no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class RegisterInntektOppgaveDTO(
    @JsonProperty("aktørId") val aktørId: String,
    @JsonProperty("referanse") val referanse: UUID,
    @JsonProperty("frist") val frist: LocalDateTime,
    @JsonProperty("fomDato") val fomDato: LocalDate,
    @JsonProperty("tomDato") val tomDato: LocalDate,
    @JsonProperty("registerInntekter") val registerInntekter: RegisterInntektDTO,
    )

data class RegisterInntektDTO(
    @JsonProperty("registerinntekterForArbeidOgFrilans") val registerinntekterForArbeidOgFrilans: List<RegisterInntektArbeidOgFrilansDTO>? = listOf(),
    @JsonProperty("registerinntekterForYtelse") val registerinntekterForYtelse: List<RegisterInntektYtelseDTO>? = listOf(),
)

data class RegisterInntektYtelseDTO (
    @JsonProperty("beløp") val beløp: Int,
    @JsonProperty("ytelseType") val ytelseType: String,
)

data class RegisterInntektArbeidOgFrilansDTO (
    @JsonProperty("beløp") val beløp: Int,
    @JsonProperty("arbeidsgiverIdent") val arbeidsgiverIdent: String,
)

