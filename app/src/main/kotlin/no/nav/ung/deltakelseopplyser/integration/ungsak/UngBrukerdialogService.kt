package no.nav.ung.deltakelseopplyser.integration.ungsak

import no.nav.ung.brukerdialog.kontrakt.oppgaver.OpprettOppgaveDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.resilience.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.ErrorResponseException
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import java.net.URI

@Service
class UngBrukerdialogService(
    private val ungBrukerdialogRetryClient: UngBrukerdialogRetryClient,
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(UngBrukerdialogService::class.java)
    }

    fun opprettSøkYtelseOppgave(opprettOppgave: OpprettOppgaveDto): Boolean {
        return try {
            ungBrukerdialogRetryClient.opprettSøkYtelseOppgave(opprettOppgave)
        } catch (exception: HttpClientErrorException) {
            if (exception.statusCode == HttpStatus.UNAUTHORIZED || exception.statusCode == HttpStatus.FORBIDDEN) {
                throw exception
            }
            logger.error("Fikk en HttpClientErrorException når man kalte opprettSøkYtelseOppgave tjeneste i ung-brukerdialog-api. Error response = '${exception.responseBodyAsString}'")
            false
        } catch (_: HttpServerErrorException) {
            logger.error("Fikk en HttpServerErrorException når man kalte opprettSøkYtelseOppgave tjeneste i ung-brukerdialog-api.")
            false
        } catch (_: ResourceAccessException) {
            logger.error("Fikk en ResourceAccessException når man kalte opprettSøkYtelseOppgave tjeneste i ung-brukerdialog-api.")
            false
        }
    }
}

@Component
@Retryable(
    excludes = [UngBrukerdialogException::class, HttpClientErrorException.Unauthorized::class, HttpClientErrorException.Forbidden::class, ResourceAccessException::class],
    maxRetriesString = "\${spring.rest.retry.maxRetries}",
    delayString = "\${spring.rest.retry.initialDelay}",
    multiplierString = "\${spring.rest.retry.multiplier}",
    maxDelayString = "\${spring.rest.retry.maxDelay}",
)
class UngBrukerdialogRetryClient(
    @Qualifier("ungBrukerdialogKlient")
    private val ungBrukerdialogKlient: RestTemplate,
) {
    private companion object {
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

