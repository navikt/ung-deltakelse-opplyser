package no.nav.ung.deltakelseopplyser.integration.enhetsregisteret

import com.fasterxml.jackson.databind.ObjectMapper
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
 * Service for å hente organisasjonsinfo fra enhetsregisteret (ereg).
 *
 * Se [EREG API V1](https://ereg-services.dev.intern.nav.no/swagger-ui/index.html) for mer informasjon.
 */
@Service
class EnhetsregisterService(
    private val enhetsregisterRetryClient: EnhetsregisterRetryClient,
    private val objectMapper: ObjectMapper,
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(EnhetsregisterService::class.java)

        const val TJENESTE_NAVN = "enhetsregisteret"
    }

    fun hentOrganisasjonsinfo(organisasjonsnummer: String): OrganisasjonRespons {
        return try {
            enhetsregisterRetryClient.hentOrganisasjonsinfo(organisasjonsnummer)
        } catch (_: HttpClientErrorException.NotFound) {
            throw EnhetsregisterException(
                "Fant ikke organisasjon med organisasjonsnummer: $organisasjonsnummer",
                HttpStatus.NOT_FOUND
            )
        } catch (ex: HttpClientErrorException) {
            val feilmelding = parseFeilmelding(ex.responseBodyAsString)
            logger.warn("Klientfeil ${ex.statusCode} mot $TJENESTE_NAVN: $feilmelding", ex)
            throw EnhetsregisterException(feilmelding, HttpStatus.valueOf(ex.statusCode.value()))
        } catch (ex: HttpServerErrorException) {
            val feilmelding = parseFeilmelding(ex.responseBodyAsString)
            logger.error("Serverfeil ${ex.statusCode} mot $TJENESTE_NAVN: $feilmelding", ex)
            throw EnhetsregisterException("Annen feil: $feilmelding", HttpStatus.valueOf(ex.statusCode.value()))
        } catch (ex: ResourceAccessException) {
            logger.error("Tilgangsfeil mot $TJENESTE_NAVN: ${ex.message}", ex)
            throw EnhetsregisterException(
                "Kunne ikke nå enhetsregisteret: ${ex.message}",
                HttpStatus.SERVICE_UNAVAILABLE
            )
        }
    }

    private fun parseFeilmelding(body: String): String =
        try {
            objectMapper.readTree(body).get("melding")?.asText() ?: body
        } catch (_: Exception) {
            body
        }
}

@Component
@Retryable(
    excludes = [
        ResourceAccessException::class,
        EnhetsregisterException::class,
        HttpClientErrorException.NotFound::class,
    ],
    maxRetriesString = "\${spring.rest.retry.maxRetries}",
    delayString = "\${spring.rest.retry.initialDelay}",
    multiplierString = "\${spring.rest.retry.multiplier}",
    maxDelayString = "\${spring.rest.retry.maxDelay}",
)
class EnhetsregisterRetryClient(
    @Qualifier("enhetsregisterKlient") private val enhetsregisterKlient: RestTemplate,
) {
    private companion object {
        private val hentOrganisasjonInfoUrl = "/v2/organisasjon"
    }

    fun hentOrganisasjonsinfo(organisasjonsnummer: String): OrganisasjonRespons {
        val response = enhetsregisterKlient.exchange(
            "$hentOrganisasjonInfoUrl/$organisasjonsnummer",
            HttpMethod.GET,
            null,
            OrganisasjonRespons::class.java
        )
        return response.body!!
    }
}

class EnhetsregisterException(
    melding: String,
    httpStatus: HttpStatus,
) : ErrorResponseException(httpStatus, asProblemDetail(melding, httpStatus), null) {
    private companion object {
        private fun asProblemDetail(
            melding: String,
            httpStatus: HttpStatus,
        ): ProblemDetail {
            val problemDetail = ProblemDetail.forStatus(httpStatus)
            problemDetail.title = "Feil ved kall mot enhetsregisteret"
            problemDetail.detail = melding

            problemDetail.type = URI("/problem-details/enhetsregisteret")

            return problemDetail
        }
    }
}
