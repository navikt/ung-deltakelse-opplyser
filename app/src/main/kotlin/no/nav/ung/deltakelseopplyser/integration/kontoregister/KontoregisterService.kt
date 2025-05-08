package no.nav.ung.deltakelseopplyser.integration.kontoregister

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.KontonummerDTO
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
    noRetryFor = [
        ResourceAccessException::class,
        KontoregisterException::class
    ],
    backoff = Backoff(
        delayExpression = "\${spring.rest.retry.initialDelay}",
        multiplierExpression = "\${spring.rest.retry.multiplier}",
        maxDelayExpression = "\${spring.rest.retry.maxDelay}"
    ),
    maxAttemptsExpression = "\${spring.rest.retry.maxAttempts}",
)
@Service
class KontoregisterService(
    @Qualifier("kontoregisterKlient") private val kontoregisterKlient: RestTemplate,
    private val objectMapper: ObjectMapper,
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(KontoregisterService::class.java)

        const val TJENESTE_NAVN = "sokos-kontoregister-person"

        private val hentAktivKontoUrl = "/api/borger/v1/hent-aktiv-konto"
    }

    fun hentAktivKonto(): KontonummerDTO {
        return kotlin.runCatching {
            kontoregisterKlient.exchange(
                hentAktivKontoUrl,
                HttpMethod.GET,
                null,
                Konto::class.java
            )
        }.fold(
            onSuccess = {
                KontonummerDTO(
                    harKontonummer = true,
                    kontonummer = it.body!!.kontonummer
                )
            },
            onFailure = {
                if (it is HttpClientErrorException.NotFound) {
                    return KontonummerDTO(harKontonummer = false)
                }
                throw it
            }
        )
    }

    @Recover
    open fun hentAktivKonto(ex: HttpClientErrorException): KontonummerDTO {
        val feilmelding = parseFeilmelding(ex.responseBodyAsString)
        logger.warn("Klientfeil ${ex.statusCode} mot $TJENESTE_NAVN: $feilmelding")

        throw KontoregisterException(feilmelding, HttpStatus.valueOf(ex.statusCode.value()))
    }

    @Recover
    open fun recoverServerError(ex: HttpServerErrorException): KontonummerDTO {
        val feilmelding = parseFeilmelding(ex.responseBodyAsString)
        logger.error("Serverfeil ${ex.statusCode} mot $TJENESTE_NAVN: $feilmelding")
        throw KontoregisterException("Annen feil: $feilmelding", HttpStatus.valueOf(ex.statusCode.value()))
    }

    @Recover
    open fun recoverResourceAccess(ex: ResourceAccessException): KontonummerDTO {
        logger.error("Tilgangsfeil mot $TJENESTE_NAVN: ${ex.message}")
        throw KontoregisterException(
            "Kunne ikke nå kontoregisteret: ${ex.message}",
            HttpStatus.SERVICE_UNAVAILABLE
        )
    }

    private fun parseFeilmelding(body: String): String =
        try {
            objectMapper.readTree(body).get("feilmelding")?.asText() ?: body
        } catch (_: Exception) {
            body
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
