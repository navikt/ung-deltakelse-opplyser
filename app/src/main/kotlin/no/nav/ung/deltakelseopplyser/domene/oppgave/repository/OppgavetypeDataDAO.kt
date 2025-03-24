package no.nav.ung.deltakelseopplyser.domene.oppgave.repository

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.ung.deltakelseopplyser.domene.oppgave.OppgavetypeDataJsonType
import java.time.LocalDate

@OppgavetypeDataJsonType
sealed class OppgavetypeDataDAO

data class EndretStartdatoOppgavetypeDataDAO(
    @JsonFormat(pattern = "yyyy-MM-dd") val nyStartdato: LocalDate,
    @JsonProperty(defaultValue = "n/a") val veilederRef: String = "n/a",
    val meldingFraVeileder: String?,
) : OppgavetypeDataDAO()

data class EndretSluttdatoOppgavetypeDataDAO(
    @JsonFormat(pattern = "yyyy-MM-dd")val nySluttdato: LocalDate,
    @JsonProperty(defaultValue = "n/a") val veilederRef: String,
    val meldingFraVeileder: String?,
) : OppgavetypeDataDAO()

data class KontrollerRegisterInntektOppgaveTypeDataDAO(
    @JsonProperty(defaultValue = "n/a") val registerinntekt: RegisterinntektDAO,
    @JsonProperty(defaultValue = "n/a") val fomDato: LocalDate,
    @JsonProperty(defaultValue = "n/a") val tomDato: LocalDate,
    ) : OppgavetypeDataDAO()


data class RegisterinntektDAO(
    @JsonProperty("arbeidOgFrilansInntekter") val arbeidOgFrilansInntekter: List<ArbeidOgFrilansRegisterInntektDAO>,
    @JsonProperty("ytelseInntekter") val ytelseInntekter: List<YtelseRegisterInntektDAO>
)

data class ArbeidOgFrilansRegisterInntektDAO(
    @JsonProperty("inntekt") val inntekt: Int,
    @JsonProperty("arbeidsgiver") val arbeidsgiver: String
)


data class YtelseRegisterInntektDAO(
    @JsonProperty("inntekt") val inntekt: Int,
    @JsonProperty("ytelsetype") val ytelsetype: String
)
