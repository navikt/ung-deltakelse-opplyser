package no.nav.ung.deltakelseopplyser.domene.oppgave

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.EndretSluttdatoOppgavetypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.EndretStartdatoOppgavetypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.KontrollerRegisterInntektOppgaveTypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgavetypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.RegisterinntektDAO
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

fun OppgavetypeDataDAO.tilDTO(): OppgavetypeDataDTO = when (this) {
    is EndretStartdatoOppgavetypeDataDAO -> EndretStartdatoOppgavetypeDataDTO(
        nyStartdato,
        veilederRef,
        meldingFraVeileder
    )

    is EndretSluttdatoOppgavetypeDataDAO -> EndretSluttdatoOppgavetypeDataDTO(
        nySluttdato,
        veilederRef,
        meldingFraVeileder
    )

    is KontrollerRegisterInntektOppgaveTypeDataDAO -> KontrollerRegisterinntektOppgavetypeDataDTO(
        fomDato,
        tomDato,
        registerinntekt.tilDTO()
    )
}

private fun RegisterinntektDAO.tilDTO(): RegisterinntektDTO {
    return RegisterinntektDTO(
        arbeidOgFrilansInntekter = arbeidOgFrilansInntekter.map {
            ArbeidOgFrilansRegisterInntektDTO(
                it.inntekt,
                it.arbeidsgiver
            )
        },
        ytelseInntekter = ytelseInntekter.map { YtelseRegisterInntektDTO(it.inntekt, it.ytelsetype) }
    )
}
