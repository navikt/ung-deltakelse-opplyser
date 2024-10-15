package no.nav.ung.deltakelseopplyser.integration.k9sak

import com.fasterxml.jackson.annotation.JsonValue
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
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Retryable(
    noRetryFor = [K9SakException::class, HttpClientErrorException.Unauthorized::class, HttpClientErrorException.Forbidden::class, ResourceAccessException::class],
    backoff = Backoff(
        delayExpression = "\${spring.rest.retry.initialDelay}",
        multiplierExpression = "\${spring.rest.retry.multiplier}",
        maxDelayExpression = "\${spring.rest.retry.maxDelay}"
    ),
    maxAttemptsExpression = "\${spring.rest.retry.maxAttempts}",

    )
class K9SakService(
    @Qualifier("k9SakKlient")
    private val k9SakKlient: RestTemplate,
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(K9SakService::class.java)

        private val hendelseInnsendingUrl = "/api/fagsak/hendelse/innsending"
    }

    fun sendInnHendelse(hendelse: K9UngdomsprogramOpphørHendelse): Boolean {
        val httpEntity = HttpEntity(hendelse)
        val response = k9SakKlient.exchange(
            hendelseInnsendingUrl,
            HttpMethod.POST,
            httpEntity,
            Unit::class.java
        )
        return response.statusCode == HttpStatus.OK
    }

    @Recover
    private fun sendInnHendelse(
        exception: HttpClientErrorException,
        hendelse: K9UngdomsprogramOpphørHendelse,
    ): Boolean {
        logger.error("Fikk en HttpClientErrorException når man kalte sendInnHendelse tjeneste i k9-sak. Error response = '${exception.responseBodyAsString}'")
        return false
    }

    @Recover
    private fun sendInnHendelse(
        exception: HttpServerErrorException,
        hendelse: K9UngdomsprogramOpphørHendelse,
    ): Boolean {
        logger.error("Fikk en HttpServerErrorException når man kalte sendInnHendelse tjeneste i k9-sak.")
        return false
    }

    @Recover
    private fun sendInnHendelse(
        exception: ResourceAccessException,
        hendelse: K9UngdomsprogramOpphørHendelse,
    ): Boolean {
        logger.error("Fikk en ResourceAccessException når man kalte sendInnHendelse tjeneste i k9-sak.")
        return false
    }

    data class K9UngdomsprogramOpphørHendelse(
        val type: HendelseType,
        val hendelseInfo: K9HendelseInfo,
        val opphørsdato: LocalDate,
    )

    data class K9HendelseInfo(
        val aktørIder: List<String>,
        val opprettet: LocalDateTime,
    )

    enum class HendelseType(@JsonValue val value: String) {
        UNGDOMSPROGRAM_OPPHØR("UNG_OPPHØR")
    }
}

class K9SakException(
    melding: String,
    httpStatus: HttpStatus,
) : ErrorResponseException(httpStatus, asProblemDetail(melding, httpStatus), null) {
    private companion object {
        private fun asProblemDetail(
            melding: String,
            httpStatus: HttpStatus,
        ): ProblemDetail {
            val problemDetail = ProblemDetail.forStatus(httpStatus)
            problemDetail.title = "Feil ved kall mot k9-sak"
            problemDetail.detail = melding

            problemDetail.type = URI("/problem-details/k9-sak")

            return problemDetail
        }
    }
}
