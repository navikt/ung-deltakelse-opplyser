package no.nav.ung.deltakelseopplyser.domene.oppgave.repository

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.ArbeidOgFrilansRegisterInntektDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.RegisterinntektDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.YtelseRegisterInntektDTO
import java.time.LocalDate

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    // Endret startdato oppgavetype data
    JsonSubTypes.Type(value = EndretStartdatoOppgavetypeDataDAO::class, name = "BEKREFT_ENDRET_STARTDATO"),

    // Endret sluttdato oppgavetype data
    JsonSubTypes.Type(value = EndretSluttdatoOppgavetypeDataDAO::class, name = "BEKREFT_ENDRET_SLUTTDATO"),

    // Kontroller registerinntekt oppgavetype data
    JsonSubTypes.Type(
        value = KontrollerRegisterInntektOppgaveTypeDataDAO::class,
        name = "BEKREFT_AVVIK_REGISTERINNTEKT"
    )
)
sealed class OppgavetypeDataDAO

data class EndretStartdatoOppgavetypeDataDAO(
    @JsonFormat(pattern = "yyyy-MM-dd") val nyStartdato: LocalDate,
    @JsonProperty(defaultValue = "n/a") val veilederRef: String = "n/a",
    val meldingFraVeileder: String?,
) : OppgavetypeDataDAO()

data class EndretSluttdatoOppgavetypeDataDAO(
    @JsonFormat(pattern = "yyyy-MM-dd") val nySluttdato: LocalDate,
    @JsonProperty(defaultValue = "n/a") val veilederRef: String,
    val meldingFraVeileder: String?,
) : OppgavetypeDataDAO()

data class KontrollerRegisterInntektOppgaveTypeDataDAO(
    @JsonProperty(defaultValue = "n/a") val registerinntekt: RegisterinntektDAO,

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty(defaultValue = "n/a")
    val fomDato: LocalDate,

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty(defaultValue = "n/a") val tomDato: LocalDate,
) : OppgavetypeDataDAO()


data class RegisterinntektDAO(
    @JsonProperty("arbeidOgFrilansInntekter") val arbeidOgFrilansInntekter: List<ArbeidOgFrilansRegisterInntektDAO>,
    @JsonProperty("ytelseInntekter") val ytelseInntekter: List<YtelseRegisterInntektDAO>,
) {
    companion object {
        fun RegisterinntektDAO.tilDTO() = RegisterinntektDTO(
            arbeidOgFrilansInntekter = arbeidOgFrilansInntekter.map {
                ArbeidOgFrilansRegisterInntektDTO(
                    it.inntekt,
                    it.arbeidsgiver
                )
            },
            ytelseInntekter = ytelseInntekter.map { YtelseRegisterInntektDTO(it.inntekt, it.ytelsetype) }
        )
    }
}

data class ArbeidOgFrilansRegisterInntektDAO(
    @JsonProperty("inntekt") val inntekt: Int,
    @JsonProperty("arbeidsgiver") val arbeidsgiver: String,
)


data class YtelseRegisterInntektDAO(
    @JsonProperty("inntekt") val inntekt: Int,
    @JsonProperty("ytelsetype") val ytelsetype: String,
)
