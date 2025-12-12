package no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.RapportertInntektPeriodeinfoDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.YtelseType
import java.time.LocalDate

@OppgavetypeDataJsonType
interface OppgavetypeDataDTO

data class EndretStartdatoDataDTO(
    @JsonProperty("nyStartdato") val nyStartdato: LocalDate,
    @JsonProperty("forrigeStartdato") val forrigeStartdato: LocalDate,
) : OppgavetypeDataDTO

data class EndretSluttdatoDataDTO(
    @JsonProperty("nySluttdato") val nySluttdato: LocalDate,
    @JsonProperty("forrigeSluttdato") val forrigeSluttdato: LocalDate? = null,
) : OppgavetypeDataDTO

data class FjernetPeriodeDataDTO(
    @JsonProperty("forrigeStartdato") val forrigeStartdato: LocalDate,
    @JsonProperty("forrigeSluttdato") val forrigeSluttdato: LocalDate? = null,
) : OppgavetypeDataDTO

data class EndretPeriodeDataDTO(
    @JsonProperty("nyPeriode") val nyPeriode: PeriodeDTO? = null,
    @JsonProperty("forrigePeriode") val forrigePeriode: PeriodeDTO? = null,
) : OppgavetypeDataDTO

data class KontrollerRegisterinntektOppgavetypeDataDTO(
    @JsonProperty("fraOgMed") val fraOgMed: LocalDate,
    @JsonProperty("tilOgMed") val tilOgMed: LocalDate,
    @JsonProperty("registerinntekt") val registerinntekt: RegisterinntektDTO,
    @JsonProperty("gjelderDelerAvMåned") val gjelderDelerAvMåned: Boolean,
    ) : OppgavetypeDataDTO

data class RegisterinntektDTO(
    @JsonProperty("arbeidOgFrilansInntekter") val arbeidOgFrilansInntekter: List<ArbeidOgFrilansRegisterInntektDTO>,
    @JsonProperty("ytelseInntekter") val ytelseInntekter: List<YtelseRegisterInntektDTO>,
    @JsonProperty("totalInntektArbeidOgFrilans") val totalInntektArbeidOgFrilans: Int = arbeidOgFrilansInntekter.sumOf { it.inntekt },
    @JsonProperty("totalInntektYtelse") val totalInntektYtelse: Int = ytelseInntekter.sumOf { it.inntekt },
    @JsonProperty("totalInntekt") val totalInntekt: Int = totalInntektArbeidOgFrilans + totalInntektYtelse,
)

data class InntektsrapporteringOppgavetypeDataDTO(
    @JsonProperty("fraOgMed") val fraOgMed: LocalDate,
    @JsonProperty("tilOgMed") val tilOgMed: LocalDate,
    @JsonProperty("rapportertInntekt") val rapportertInntekt: RapportertInntektPeriodeinfoDTO? = null,
    @JsonProperty("gjelderDelerAvMåned") val gjelderDelerAvMåned: Boolean,

    ) : OppgavetypeDataDTO

data class SøkYtelseOppgavetypeDataDTO(
    @JsonProperty("fomDato") val fomDato: LocalDate,
) : OppgavetypeDataDTO

data class ArbeidOgFrilansRegisterInntektDTO(
    @JsonProperty("inntekt") val inntekt: Int,
    @JsonProperty("arbeidsgiver") val arbeidsgiver: String,
    @JsonProperty("arbeidsgiverNavn") val arbeidsgiverNavn: String?,
)


data class YtelseRegisterInntektDTO(
    @JsonProperty("inntekt") val inntekt: Int,
    @JsonProperty("ytelsetype") val ytelsetype: YtelseType,
)
