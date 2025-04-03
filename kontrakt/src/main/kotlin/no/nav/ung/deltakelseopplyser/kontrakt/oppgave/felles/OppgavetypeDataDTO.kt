package no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles

import com.fasterxml.jackson.annotation.JsonProperty

import java.time.LocalDate

@OppgavetypeDataJsonType
interface OppgavetypeDataDTO

data class EndretStartdatoOppgavetypeDataDTO(
    @JsonProperty("nyStartdato") val nyStartdato: LocalDate,
) : OppgavetypeDataDTO

data class EndretSluttdatoOppgavetypeDataDTO(
    @JsonProperty("nySluttdato") val nySluttdato: LocalDate
) : OppgavetypeDataDTO

data class KontrollerRegisterinntektOppgavetypeDataDTO(
    @JsonProperty("fraOgMed") val fraOgMed: LocalDate,
    @JsonProperty("tilOgMed") val tilOgMed: LocalDate,
    @JsonProperty("registerinntekt") val registerinntekt: RegisterinntektDTO,
) : OppgavetypeDataDTO

data class RegisterinntektDTO(
    @JsonProperty("arbeidOgFrilansInntekter") val arbeidOgFrilansInntekter: List<ArbeidOgFrilansRegisterInntektDTO>,
    @JsonProperty("ytelseInntekter") val ytelseInntekter: List<YtelseRegisterInntektDTO>,
    @JsonProperty("totalInntektArbeidOgFrilans") val totalInntektArbeidOgFrilans: Int = arbeidOgFrilansInntekter.sumOf { it.inntekt },
    @JsonProperty("totalInntektYtelse") val totalInntektYtelse: Int = ytelseInntekter.sumOf { it.inntekt },
    @JsonProperty("totalInntekt") val totalInntekt: Int = totalInntektArbeidOgFrilans + totalInntektYtelse,
)

data class ArbeidOgFrilansRegisterInntektDTO(
    @JsonProperty("inntekt") val inntekt: Int,
    @JsonProperty("arbeidsgiver") val arbeidsgiver: String,
)


data class YtelseRegisterInntektDTO(
    @JsonProperty("inntekt") val inntekt: Int,
    @JsonProperty("ytelsetype") val ytelsetype: String,
)
