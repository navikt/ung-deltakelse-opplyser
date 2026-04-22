package no.nav.ung.deltakelseopplyser.integration.ungsak

import no.nav.ung.sak.kontrakt.hendelser.HendelseDto
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
class UngSakService(
    private val ungSakRetryClient: UngSakRetryClient,
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(UngSakService::class.java)
    }

    fun sendInnHendelse(hendelse: HendelseDto): Boolean {
        return try {
            ungSakRetryClient.sendInnHendelse(hendelse)
        } catch (exception: HttpClientErrorException) {
            if (exception is HttpClientErrorException.Unauthorized || exception is HttpClientErrorException.Forbidden) {
                throw exception
            }
            logger.error("Fikk en HttpClientErrorException når man kalte sendInnHendelse tjeneste i ung-sak. Error response = '${exception.responseBodyAsString}'")
            false
        } catch (_: HttpServerErrorException) {
            logger.error("Fikk en HttpServerErrorException når man kalte sendInnHendelse tjeneste i ung-sak.")
            false
        } catch (_: ResourceAccessException) {
            logger.error("Fikk en ResourceAccessException når man kalte sendInnHendelse tjeneste i ung-sak.")
            false
        }
    }
}

@Component
@Retryable(
    excludes = [UngSakException::class, HttpClientErrorException.Unauthorized::class, HttpClientErrorException.Forbidden::class, ResourceAccessException::class],
    maxRetriesString = "\${spring.rest.retry.maxRetries}",
    delayString = "\${spring.rest.retry.initialDelay}",
    multiplierString = "\${spring.rest.retry.multiplier}",
    maxDelayString = "\${spring.rest.retry.maxDelay}",
)
class UngSakRetryClient(
    @Qualifier("ungSakKlient")
    private val ungSakKlient: RestTemplate,
) {
    private companion object {
        private val hendelseInnsendingUrl = "/api/fagsak/hendelse/innsending"
    }

    fun sendInnHendelse(hendelse: HendelseDto): Boolean {
        val httpEntity = HttpEntity(hendelse)
        val response = ungSakKlient.exchange(
            hendelseInnsendingUrl,
            HttpMethod.POST,
            httpEntity,
            Unit::class.java
        )
        return response.statusCode == HttpStatus.OK
    }
}

class UngSakException(
    melding: String,
    httpStatus: HttpStatus,
) : ErrorResponseException(httpStatus, asProblemDetail(melding, httpStatus), null) {
    private companion object {
        private fun asProblemDetail(
            melding: String,
            httpStatus: HttpStatus,
        ): ProblemDetail {
            val problemDetail = ProblemDetail.forStatus(httpStatus)
            problemDetail.title = "Feil ved kall mot ung-sak"
            problemDetail.detail = melding

            problemDetail.type = URI("/problem-details/ung-sak")

            return problemDetail
        }
    }
}

