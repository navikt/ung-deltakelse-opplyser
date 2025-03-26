package no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles

import com.fasterxml.jackson.annotation.JsonProperty

import java.time.LocalDate

@OppgavetypeDataJsonType
interface OppgavetypeDataDTO

data class EndretStartdatoOppgavetypeDataDTO(
    val nyStartdato: LocalDate,
    val veilederRef: String,
    val meldingFraVeileder: String?,
) : OppgavetypeDataDTO

data class EndretSluttdatoOppgavetypeDataDTO(
    val nySluttdato: LocalDate,
    val veilederRef: String,
    val meldingFraVeileder: String?,
) : OppgavetypeDataDTO

data class KontrollerRegisterinntektOppgavetypeDataDTO(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val registerinntekt: RegisterinntektDTO,
) : OppgavetypeDataDTO

data class RegisterinntektDTO(
    val arbeidOgFrilansInntekter: List<ArbeidOgFrilansRegisterInntektDTO>,
    val ytelseInntekter: List<YtelseRegisterInntektDTO>,
    val totalInntektArbeidOgFrilans: Int = arbeidOgFrilansInntekter.sumOf { it.inntekt },
    val totalInntektYtelse: Int = ytelseInntekter.sumOf { it.inntekt },
    val totalInntekt: Int = totalInntektArbeidOgFrilans + totalInntektYtelse,
)

data class ArbeidOgFrilansRegisterInntektDTO(
    @JsonProperty("inntekt") val inntekt: Int,
    @JsonProperty("arbeidsgiver") val arbeidsgiver: String,
)


data class YtelseRegisterInntektDTO(
    @JsonProperty("inntekt") val inntekt: Int,
    @JsonProperty("ytelsetype") val ytelsetype: String,
)
