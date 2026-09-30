package no.nav.ung.deltakelseopplyser.integration.kontoregister

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.KontonummerDTO
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
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

/**
 * Service for å hente kontonummer fra kontoregisteret.
 *
 * Se [Kontoregister borger-API](https://sokos-kontoregister-person.intern.dev.nav.no/api/borger/v1/docs/#/kontoregister.v1) for mer informasjon.
 */
@Service
class KontoregisterService(
    private val kontoregisterRetryClient: KontoregisterRetryClient,
    private val objectMapper: ObjectMapper,
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(KontoregisterService::class.java)

        const val TJENESTE_NAVN = "sokos-kontoregister-person"
    }

    fun hentAktivKonto(): KontonummerDTO {
        return try {
            val konto = kontoregisterRetryClient.hentAktivKonto()
            KontonummerDTO(
                harKontonummer = true,
                kontonummer = konto.kontonummer
            )
        } catch (_: HttpClientErrorException.NotFound) {
            KontonummerDTO(harKontonummer = false)
        } catch (ex: HttpClientErrorException) {
            val feilmelding = parseFeilmelding(ex.responseBodyAsString)
            logger.warn("Klientfeil ${ex.statusCode} mot $TJENESTE_NAVN: $feilmelding")
            throw KontoregisterException(feilmelding, HttpStatus.valueOf(ex.statusCode.value()))
        } catch (ex: HttpServerErrorException) {
            val feilmelding = parseFeilmelding(ex.responseBodyAsString)
            logger.error("Serverfeil ${ex.statusCode} mot $TJENESTE_NAVN: $feilmelding")
            throw KontoregisterException("Annen feil: $feilmelding", HttpStatus.valueOf(ex.statusCode.value()))
        } catch (ex: ResourceAccessException) {
            logger.error("Tilgangsfeil mot $TJENESTE_NAVN: ${ex.message}")
            throw KontoregisterException(
                "Kunne ikke nå kontoregisteret: ${ex.message}",
                HttpStatus.SERVICE_UNAVAILABLE
            )
        }
    }

    private fun parseFeilmelding(body: String): String =
        try {
            objectMapper.readTree(body).get("feilmelding")?.asText() ?: body
        } catch (_: Exception) {
            body
        }
}

@Component
@Retryable(
    excludes = [
        ResourceAccessException::class,
        KontoregisterException::class,
        HttpClientErrorException.NotFound::class,
    ],
    maxRetriesString = "\${spring.rest.retry.maxRetries}",
    delayString = "\${spring.rest.retry.initialDelay}",
    multiplierString = "\${spring.rest.retry.multiplier}",
    maxDelayString = "\${spring.rest.retry.maxDelay}",
)
class KontoregisterRetryClient(
    @Qualifier("kontoregisterKlient") private val kontoregisterKlient: RestTemplate,
) {
    private companion object {
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
