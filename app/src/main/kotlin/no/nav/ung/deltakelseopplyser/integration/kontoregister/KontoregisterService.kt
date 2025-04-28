package no.nav.ung.deltakelseopplyser.integration.kontoregister

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
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

/**
 * Service for å hente kontonummer fra kontoregisteret.
 *
 * Se [Kontoregister borger-API](https://sokos-kontoregister-person.intern.dev.nav.no/api/borger/v1/docs/#/kontoregister.v1) for mer informasjon.
 */

@Retryable(
    noRetryFor = [KontoregisterException::class, HttpClientErrorException.Unauthorized::class, HttpClientErrorException.Forbidden::class, ResourceAccessException::class],
    backoff = Backoff(
        delayExpression = "\${spring.rest.retry.initialDelay}",
        multiplierExpression = "\${spring.rest.retry.multiplier}",
        maxDelayExpression = "\${spring.rest.retry.maxDelay}"
    ),
    maxAttemptsExpression = "\${spring.rest.retry.maxAttempts}",

    )
@Service
class KontoregisterService(@Qualifier("kontoregisterKlient") private val kontoregisterKlient: RestTemplate) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(KontoregisterService::class.java)

        const val TJENESTE_NAVN = "sokos-kontoregister-person"

        private val hentAktivKontoUrl = "/api/borger/v1/hent-aktiv-konto"
    }

    fun hentAktivKonto(): Konto {
        val response = kontoregisterKlient.exchange(
            hentAktivKontoUrl,
            HttpMethod.GET,
            null,
            Konto::class.java
        )
        return response.body!!
    }

    @Recover
    private fun hentAktivKonto(
        exception: HttpClientErrorException,
    ): Konto {
        logger.error("Fikk en HttpClientErrorException ved kall mot $TJENESTE_NAVN. Error response = '${exception.responseBodyAsString}'")
        throw exception
    }

    @Recover
    private fun hentAktivKonto(
        exception: HttpServerErrorException,
    ): Konto {
        logger.error("Fikk en HttpServerErrorException ved oppslag mot $TJENESTE_NAVN. Error response = '${exception.responseBodyAsString}'")
        throw exception
    }

    @Recover
    private fun hentAktivKonto(
        exception: ResourceAccessException,
    ): Konto {
        logger.error("Fikk en ResourceAccessException oppslag mot $TJENESTE_NAVN. Error response = '${exception.message}'")
        throw exception
    }
}

data class Konto(val kontonummer: String)

class KontoregisterException(
    melding: String,
    httpStatus: HttpStatus,
) : ErrorResponseException(httpStatus, asProblemDetail(melding, httpStatus), null) {
    private companion object {
        private fun asProblemDetail(
            melding: String,
            httpStatus: HttpStatus,
        ): ProblemDetail {
            val problemDetail = ProblemDetail.forStatus(httpStatus)
            problemDetail.title = "Feil ved kall mot sokos-kontoregister-person"
            problemDetail.detail = melding

            problemDetail.type = URI("/problem-details/sokos-kontoregister-person")

            return problemDetail
        }
    }
}
