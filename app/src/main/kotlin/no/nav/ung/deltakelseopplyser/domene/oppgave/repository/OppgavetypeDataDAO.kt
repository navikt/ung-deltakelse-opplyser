package no.nav.ung.deltakelseopplyser.domene.oppgave.repository

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.ArbeidOgFrilansRegisterInntektDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.RegisterinntektDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.YtelseRegisterInntektDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.YtelseType
import java.time.LocalDate

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    // Endret programperiode oppgavetype data
    JsonSubTypes.Type(value = EndretProgramperiodeOppgavetypeDataDAO::class, name = "BEKREFT_ENDRET_PROGRAMPERIODE"),

    // Kontroller registerinntekt oppgavetype data
    JsonSubTypes.Type(
        value = KontrollerRegisterInntektOppgaveTypeDataDAO::class,
        name = "BEKREFT_AVVIK_REGISTERINNTEKT"
    ),

    // Inntektsrapportering oppgavetype data
    JsonSubTypes.Type(
        value = InntektsrapporteringOppgavetypeDataDAO::class,
        name = "RAPPORTER_INNTEKT"
    )
)
sealed class OppgavetypeDataDAO

data class EndretProgramperiodeOppgavetypeDataDAO(
    @JsonProperty("programperiode") val programperiode: ProgramperiodeDAO,
    @JsonProperty("forrigeProgramperiode") val forrigeProgramperiode: ProgramperiodeDAO? = null,
) : OppgavetypeDataDAO()

data class ProgramperiodeDAO (
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("fomDato") val fomDato: LocalDate,

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("tomDato") val tomDato: LocalDate? = null,
)

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

data class InntektsrapporteringOppgavetypeDataDAO(
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty(defaultValue = "n/a")
    val fomDato: LocalDate,

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty(defaultValue = "n/a") val tomDato: LocalDate,
): OppgavetypeDataDAO()

data class ArbeidOgFrilansRegisterInntektDAO(
    @JsonProperty("inntekt") val inntekt: Int,
    @JsonProperty("arbeidsgiver") val arbeidsgiver: String,
)


data class YtelseRegisterInntektDAO(
    @JsonProperty("inntekt") val inntekt: Int,
    @JsonProperty("ytelsetype") val ytelsetype: YtelseType,
)

