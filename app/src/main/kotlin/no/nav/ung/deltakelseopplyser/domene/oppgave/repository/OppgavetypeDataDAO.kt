package no.nav.ung.deltakelseopplyser.domene.oppgave.repository

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tms.varsel.action.Tekst
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.ArbeidOgFrilansRegisterInntektDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.RegisterinntektDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.YtelseRegisterInntektDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.YtelseType
import no.nav.ung.deltakelseopplyser.utils.DateUtils.måned
import java.time.LocalDate

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    // Endret startdato oppgavetype data
    JsonSubTypes.Type(value = EndretStartdatoOppgaveDataDAO::class, name = "BEKREFT_ENDRET_STARTDATO"),

    // Endret sluttdato oppgavetype data
    JsonSubTypes.Type(value = EndretSluttdatoOppgaveDataDAO::class, name = "BEKREFT_ENDRET_SLUTTDATO"),

    // Kontroller registerinntekt oppgavetype data
    JsonSubTypes.Type(
        value = KontrollerRegisterInntektOppgaveTypeDataDAO::class,
        name = "BEKREFT_AVVIK_REGISTERINNTEKT"
    ),

    // Inntektsrapportering oppgavetype data
    JsonSubTypes.Type(value = InntektsrapporteringOppgavetypeDataDAO::class, name = "RAPPORTER_INNTEKT"),

    // Send søknad oppgavetype data
    JsonSubTypes.Type(value = SøkYtelseOppgavetypeDataDAO::class, name = "SØK_YTELSE")
)
sealed class OppgavetypeDataDAO {

    fun minSideVarselTekster(): List<Tekst> = when (this) {
        is KontrollerRegisterInntektOppgaveTypeDataDAO -> listOf(
            Tekst(
                tekst = "Du har fått en oppgave om å bekrefte inntekten din",
                spraakkode = "nb",
                default = true
            )
        )

        is EndretStartdatoOppgaveDataDAO -> listOf(
            Tekst(
                tekst = "Se og gi tilbakemelding på ny startdato i ungdomsprogrammet",
                spraakkode = "nb",
                default = true
            )
        )

        is EndretSluttdatoOppgaveDataDAO -> listOf(
            Tekst(
                tekst = "Se og gi tilbakemelding på ${if (this.forrigeSluttdato == null) "" else "ny"} sluttdato i ungdomsprogrammet",
                spraakkode = "nb",
                default = true
            )
        )

        is InntektsrapporteringOppgavetypeDataDAO -> listOf(
            Tekst(
                tekst = "Du har fått en oppgave om å registrere inntekten din for ${fomDato.måned()} dersom du har det.",
                spraakkode = "nb",
                default = true
            )
        )

        is SøkYtelseOppgavetypeDataDAO -> listOf(
            Tekst(
                tekst = "Søk om ungdomsprogramytelsen",
                spraakkode = "nb",
                default = true
            )
        )
    }
}

data class EndretStartdatoOppgaveDataDAO(
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("nyStartdato") val nyStartdato: LocalDate,

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("forrigeStartdato") val forrigeStartdato: LocalDate,
) : OppgavetypeDataDAO()

data class EndretSluttdatoOppgaveDataDAO(
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("nySluttdato") val nySluttdato: LocalDate,

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("forrigeSluttdato") val forrigeSluttdato: LocalDate? = null,
) : OppgavetypeDataDAO()

data class ProgramperiodeDAO(
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

data class SøkYtelseOppgavetypeDataDAO(
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("fomDato") val fomDato: LocalDate,
) : OppgavetypeDataDAO()

data class RegisterinntektDAO(
    @JsonProperty("arbeidOgFrilansInntekter") val arbeidOgFrilansInntekter: List<ArbeidOgFrilansRegisterInntektDAO>,
    @JsonProperty("ytelseInntekter") val ytelseInntekter: List<YtelseRegisterInntektDAO>,
)

data class InntektsrapporteringOppgavetypeDataDAO(
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty(defaultValue = "n/a")
    val fomDato: LocalDate,

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty(defaultValue = "n/a") val tomDato: LocalDate,
) : OppgavetypeDataDAO()

data class ArbeidOgFrilansRegisterInntektDAO(
    @JsonProperty("inntekt") val inntekt: Int,
    @JsonProperty("arbeidsgiver") val arbeidsgiver: String,
)

data class YtelseRegisterInntektDAO(
    @JsonProperty("inntekt") val inntekt: Int,
    @JsonProperty("ytelsetype") val ytelsetype: YtelseType,
)

