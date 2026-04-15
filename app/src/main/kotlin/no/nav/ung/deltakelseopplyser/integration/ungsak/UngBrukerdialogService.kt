package no.nav.ung.deltakelseopplyser.integration.ungsak

import no.nav.ung.brukerdialog.kontrakt.oppgaver.OpprettOppgaveDto
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

        private const val opprettSøkYtelseUrl = "/intern/api/oppgavebehandling/opprett"
    }

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
        logger.error("Fikk en HttpClientErrorException når man kalte opprettSøkYtelseOppgave tjeneste i ung-brukerdialog-api. Error response = '${exception.responseBodyAsString}'")
        return false
    }

    @Recover
    fun opprettSøkYtelseOppgave(
        exception: HttpServerErrorException,
        opprettOppgave: OpprettOppgaveDto,
    ): Boolean {
        logger.error("Fikk en HttpServerErrorException når man kalte opprettSøkYtelseOppgave tjeneste i ung-brukerdialog-api.")
        return false
    }

    @Recover
    fun opprettSøkYtelseOppgave(
        exception: ResourceAccessException,
        opprettOppgave: OpprettOppgaveDto,
    ): Boolean {
        logger.error("Fikk en ResourceAccessException når man kalte opprettSøkYtelseOppgave tjeneste i ung-brukerdialog-api.")
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
            problemDetail.title = "Feil ved kall mot ung-brukerdialog-api"
            problemDetail.detail = melding

            problemDetail.type = URI("/problem-details/ung-brukerdialog-api")

            return problemDetail
        }
    }
}

