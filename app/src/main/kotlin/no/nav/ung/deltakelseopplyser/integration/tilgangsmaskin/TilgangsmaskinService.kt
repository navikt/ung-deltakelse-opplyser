package no.nav.ung.deltakelseopplyser.integration.tilgangsmaskin

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.sif.abac.kontrakt.person.PersonIdent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange

@Service
@Retryable(
    noRetryFor = [HttpClientErrorException.Forbidden::class, HttpClientErrorException.NotFound::class, ResourceAccessException::class],
    backoff = Backoff(
        delayExpression = "\${spring.rest.retry.initialDelay}",
        multiplierExpression = "\${spring.rest.retry.multiplier}",
        maxDelayExpression = "\${spring.rest.retry.maxDelay}",
    ),
    maxAttemptsExpression = "\${spring.rest.retry.maxAttempts}",
)
class TilgangsmaskinService(
    @Qualifier("tilgangsmaskinKlient") private val tilgangsmaskinKlient: RestTemplate,
    private val objectMapper: ObjectMapper,
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(TilgangsmaskinService::class.java)
        const val KJERNE_PATH = "/api/v1/komplett"
    }

    fun evaluerKomplettRegler(brukerIdent: PersonIdent): TilgangsmaskinBeslutning {
        return try {
            val response = tilgangsmaskinKlient.exchange<Unit>(
                KJERNE_PATH,
                HttpMethod.POST,
                HttpEntity(brukerIdent.ident),
            )

            if (response.statusCode == HttpStatus.NO_CONTENT) {
                TilgangsmaskinBeslutning(harTilgang = true)
            } else {
                logger.warn("Uventet HTTP-status fra tilgangsmaskin: {}", response.statusCode)
                TilgangsmaskinBeslutning(
                    harTilgang = false,
                    avvisningsAarsak = "UVENTET_STATUS_${response.statusCode.value()}",
                )
            }
        } catch (forbidden: HttpClientErrorException.Forbidden) {
            val detail = parseProblemDetail(forbidden.responseBodyAsString)
            TilgangsmaskinBeslutning(
                harTilgang = false,
                avvisningsAarsak = detail.title ?: "AVVIST",
                begrunnelse = detail.begrunnelse,
            )
        } catch (notFound: HttpClientErrorException.NotFound) {
            val detail = parseProblemDetail(notFound.responseBodyAsString)
            TilgangsmaskinBeslutning(
                harTilgang = false,
                avvisningsAarsak = detail.title ?: "IKKE_FUNNET",
                begrunnelse = detail.detail,
            )
        }
    }

    private fun parseProblemDetail(body: String?): TilgangsmaskinProblemDetail {
        if (body.isNullOrBlank()) return TilgangsmaskinProblemDetail()

        return runCatching { objectMapper.readValue(body, TilgangsmaskinProblemDetail::class.java) }
            .getOrElse {
                logger.warn("Klarte ikke parse problemDetail-respons fra tilgangsmaskin")
                TilgangsmaskinProblemDetail(detail = body)
            }
    }
}

data class TilgangsmaskinBeslutning(
    val harTilgang: Boolean,
    val avvisningsAarsak: String? = null,
    val begrunnelse: String? = null,
)

data class TilgangsmaskinProblemDetail(
    val title: String? = null,
    val detail: String? = null,
    val begrunnelse: String? = null,
)

