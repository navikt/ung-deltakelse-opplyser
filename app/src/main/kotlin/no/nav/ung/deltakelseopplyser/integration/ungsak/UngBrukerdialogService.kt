package no.nav.ung.deltakelseopplyser.integration.ungsak

import no.nav.ung.brukerdialog.kontrakt.oppgaver.*
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.endretperiode.EndretPeriodeDataDto
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.endretsluttdato.EndretSluttdatoDataDto
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.endretstartdato.EndretStartdatoDataDto
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.fjernperiode.FjernetPeriodeDataDto
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.inntektsrapportering.InntektsrapporteringOppgavetypeDataDto
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.kontrollerregisterinntekt.KontrollerRegisterinntektOppgavetypeDataDto
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.søkytelse.SøkYtelseOppgavetypeDataDto
import no.nav.ung.brukerdialog.typer.AktørId
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.*
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.BekreftelseDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Recover
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.ErrorResponseException
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import java.net.URI
import no.nav.ung.brukerdialog.kontrakt.oppgaver.BekreftelseDTO as BrukerdialogBekreftelseDTO
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OppgaveStatus as BrukerdialogOppgaveStatus
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OppgaveType as BrukerdialogOppgaveType
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.endretperiode.PeriodeDTO as BrukerdialogPeriodeDTO
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.endretperiode.PeriodeEndringType as BrukerdialogPeriodeEndringType
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.kontrollerregisterinntekt.ArbeidOgFrilansRegisterInntektDTO as BrukerdialogArbeidOgFrilansRegisterInntektDTO
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.kontrollerregisterinntekt.RegisterinntektDTO as BrukerdialogRegisterinntektDTO
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.kontrollerregisterinntekt.YtelseRegisterInntektDTO as BrukerdialogYtelseRegisterInntektDTO
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.kontrollerregisterinntekt.YtelseType as BrukerdialogYtelseType

@Service
@Retryable(
    noRetryFor = [UngBrukerdialogException::class, HttpClientErrorException.Unauthorized::class, HttpClientErrorException.Forbidden::class, ResourceAccessException::class],
    backoff = Backoff(
        delayExpression = "\${spring.rest.retry.initialDelay}",
        multiplierExpression = "\${spring.rest.retry.multiplier}",
        maxDelayExpression = "\${spring.rest.retry.maxDelay}"
    ),
    maxAttemptsExpression = "\${spring.rest.retry.maxAttempts}",

    )
class UngBrukerdialogService(
    @Qualifier("ungBrukerdialogKlient")
    private val ungBrukerdialogKlient: RestTemplate
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(UngBrukerdialogService::class.java)

        private const val opprettSøkYtelseUrl = "/api/oppgave/opprett/sok-ytelse"
        private const val migrerOppgaverUrl = "/forvaltning/oppgave/migrer"
    }

    fun migrerOppgaver(aktørId: String, oppgaver: List<OppgaveDTO>): MigreringsResultat {
        val migrerOppgaveDtoer = oppgaver.map { oppgave ->
            MigrerOppgaveDto(
                oppgave.oppgaveReferanse,
                AktørId(aktørId),
                mapOppgavetype(oppgave.oppgavetype),
                mapOppgavetypeData(oppgave.oppgavetypeData),
                mapBekreftelse(oppgave.bekreftelse),
                mapOppgaveStatus(oppgave.status),
                oppgave.opprettetDato,
                oppgave.løstDato,
                oppgave.åpnetDato,
                oppgave.lukketDato,
                oppgave.frist
            )
        }
        val httpEntity = HttpEntity(MigreringsRequest(migrerOppgaveDtoer))
        val response = ungBrukerdialogKlient.exchange(
            migrerOppgaverUrl,
            HttpMethod.POST,
            httpEntity,
            MigreringsResultat::class.java
        )
        return response.body ?: MigreringsResultat(0, oppgaver.size)
    }

    @Recover
    fun migrerOppgaver(
        exception: HttpClientErrorException,
        aktørId: String,
        oppgaver: List<OppgaveDTO>,
    ): MigreringsResultat {
        logger.error("Fikk en HttpClientErrorException når man kalte migrerOppgaver tjeneste i ung-brukerdialog. Error response = '${exception.responseBodyAsString}'")
        return MigreringsResultat(0, oppgaver.size)
    }

    @Recover
    fun migrerOppgaver(
        exception: HttpServerErrorException,
        aktørId: String,
        oppgaver: List<OppgaveDTO>,
    ): MigreringsResultat {
        logger.error("Fikk en HttpServerErrorException når man kalte migrerOppgaver tjeneste i ung-brukerdialog.")
        return MigreringsResultat(0, oppgaver.size)
    }

    @Recover
    fun migrerOppgaver(
        exception: ResourceAccessException,
        aktørId: String,
        oppgaver: List<OppgaveDTO>,
    ): MigreringsResultat {
        logger.error("Fikk en ResourceAccessException når man kalte migrerOppgaver tjeneste i ung-brukerdialog.")
        return MigreringsResultat(0, oppgaver.size)
    }

    private fun mapOppgavetype(oppgavetype: Oppgavetype): BrukerdialogOppgaveType = when (oppgavetype) {
        Oppgavetype.BEKREFT_ENDRET_STARTDATO -> BrukerdialogOppgaveType.BEKREFT_ENDRET_STARTDATO
        Oppgavetype.BEKREFT_ENDRET_SLUTTDATO -> BrukerdialogOppgaveType.BEKREFT_ENDRET_SLUTTDATO
        Oppgavetype.BEKREFT_ENDRET_PERIODE -> BrukerdialogOppgaveType.BEKREFT_ENDRET_PERIODE
        Oppgavetype.BEKREFT_FJERNET_PERIODE -> BrukerdialogOppgaveType.BEKREFT_FJERNET_PERIODE
        Oppgavetype.BEKREFT_AVVIK_REGISTERINNTEKT -> BrukerdialogOppgaveType.BEKREFT_AVVIK_REGISTERINNTEKT
        Oppgavetype.RAPPORTER_INNTEKT -> BrukerdialogOppgaveType.RAPPORTER_INNTEKT
        Oppgavetype.SØK_YTELSE -> BrukerdialogOppgaveType.SØK_YTELSE
    }

    private fun mapOppgaveStatus(status: OppgaveStatus): BrukerdialogOppgaveStatus = when (status) {
        OppgaveStatus.LØST -> BrukerdialogOppgaveStatus.LØST
        OppgaveStatus.ULØST -> BrukerdialogOppgaveStatus.ULØST
        OppgaveStatus.AVBRUTT -> BrukerdialogOppgaveStatus.AVBRUTT
        OppgaveStatus.UTLØPT -> BrukerdialogOppgaveStatus.UTLØPT
        OppgaveStatus.LUKKET -> BrukerdialogOppgaveStatus.LUKKET
    }

    private fun mapBekreftelse(bekreftelse: BekreftelseDTO?): BrukerdialogBekreftelseDTO? =
        bekreftelse?.let { SvarPåVarselDTO(it.harUttalelse, it.uttalelseFraBruker) }

    private fun mapOppgavetypeData(oppgavetypeData: OppgavetypeDataDTO): OppgavetypeDataDto = when (oppgavetypeData) {
        is KontrollerRegisterinntektOppgavetypeDataDTO -> KontrollerRegisterinntektOppgavetypeDataDto(
            oppgavetypeData.fraOgMed,
            oppgavetypeData.tilOgMed,
            mapRegisterinntekt(oppgavetypeData),
            oppgavetypeData.gjelderDelerAvMåned
        )

        is EndretStartdatoDataDTO -> EndretStartdatoDataDto(
            oppgavetypeData.nyStartdato,
            oppgavetypeData.forrigeStartdato
        )

        is EndretSluttdatoDataDTO -> EndretSluttdatoDataDto(
            oppgavetypeData.nySluttdato,
            oppgavetypeData.forrigeSluttdato
        )

        is EndretPeriodeDataDTO -> EndretPeriodeDataDto(
            mapPeriode(oppgavetypeData.nyPeriode),
            mapPeriode(oppgavetypeData.forrigePeriode),
            oppgavetypeData.endringer.map { BrukerdialogPeriodeEndringType.valueOf(it.name) }.toSet()
        )

        is FjernetPeriodeDataDTO -> FjernetPeriodeDataDto(
            oppgavetypeData.forrigeStartdato,
            oppgavetypeData.forrigeSluttdato
        )

        is InntektsrapporteringOppgavetypeDataDTO -> InntektsrapporteringOppgavetypeDataDto(
            oppgavetypeData.fraOgMed,
            oppgavetypeData.tilOgMed,
            oppgavetypeData.gjelderDelerAvMåned
        )

        is SøkYtelseOppgavetypeDataDTO -> SøkYtelseOppgavetypeDataDto(
            oppgavetypeData.fomDato
        )

        else -> throw IllegalStateException("Ukjent oppgavetypedata: ${oppgavetypeData::class.simpleName}")
    }

    private fun mapRegisterinntekt(data: KontrollerRegisterinntektOppgavetypeDataDTO): BrukerdialogRegisterinntektDTO {
        val registerinntekt = data.registerinntekt
        return BrukerdialogRegisterinntektDTO(
            registerinntekt.arbeidOgFrilansInntekter.map {
                BrukerdialogArbeidOgFrilansRegisterInntektDTO(it.inntekt, it.arbeidsgiver, it.arbeidsgiverNavn)
            },
            registerinntekt.ytelseInntekter.map {
                BrukerdialogYtelseRegisterInntektDTO(it.inntekt, BrukerdialogYtelseType.valueOf(it.ytelsetype.name))
            },
            registerinntekt.totalInntektArbeidOgFrilans,
            registerinntekt.totalInntektYtelse,
            registerinntekt.totalInntekt
        )
    }

    private fun mapPeriode(periode: PeriodeDTO?): BrukerdialogPeriodeDTO? =
        periode?.let { BrukerdialogPeriodeDTO(it.fom, it.tom) }

    fun opprettSøkYtelseOppgave(opprettOppgave: OpprettOppgaveDto): Boolean {
        val httpEntity = HttpEntity(opprettOppgave)
        val response = ungBrukerdialogKlient.exchange(
            opprettSøkYtelseUrl,
            HttpMethod.POST,
            httpEntity,
            Unit::class.java
        )
        return response.statusCode == HttpStatus.OK
    }

    @Recover
    fun opprettSøkYtelseOppgave(
        exception: HttpClientErrorException,
        opprettOppgave: OpprettOppgaveDto,
    ): Boolean {
        logger.error("Fikk en HttpClientErrorException når man kalte opprettSøkYtelseOppgave tjeneste i ung-brukerdialog. Error response = '${exception.responseBodyAsString}'")
        return false
    }

    @Recover
    fun opprettSøkYtelseOppgave(
        exception: HttpServerErrorException,
        opprettOppgave: OpprettOppgaveDto,
    ): Boolean {
        logger.error("Fikk en HttpServerErrorException når man kalte opprettSøkYtelseOppgave tjeneste i ung-brukerdialog.")
        return false
    }

    @Recover
    fun opprettSøkYtelseOppgave(
        exception: ResourceAccessException,
        opprettOppgave: OpprettOppgaveDto,
    ): Boolean {
        logger.error("Fikk en ResourceAccessException når man kalte opprettSøkYtelseOppgave tjeneste i ung-brukerdialog.")
        return false
    }


}

class UngBrukerdialogException(
    melding: String,
    httpStatus: HttpStatus,
) : ErrorResponseException(httpStatus, asProblemDetail(melding, httpStatus), null) {
    private companion object {
        private fun asProblemDetail(
            melding: String,
            httpStatus: HttpStatus,
        ): ProblemDetail {
            val problemDetail = ProblemDetail.forStatus(httpStatus)
            problemDetail.title = "Feil ved kall mot ung-brukerdialog"
            problemDetail.detail = melding

            problemDetail.type = URI("/problem-details/ung-brukerdialog")

            return problemDetail
        }
    }
}

