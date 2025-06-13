package no.nav.ung.deltakelseopplyser.integration.abac

import no.nav.sif.abac.kontrakt.abac.dto.UngdomsprogramTilgangskontrollInputDto
import no.nav.sif.abac.kontrakt.abac.resultat.Tilgangsbeslutning
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate

@Service
@Retryable(
    noRetryFor = [HttpClientErrorException.Unauthorized::class, HttpClientErrorException.Forbidden::class, ResourceAccessException::class],
    backoff = Backoff(
        delayExpression = "\${spring.rest.retry.initialDelay}",
        multiplierExpression = "\${spring.rest.retry.multiplier}",
        maxDelayExpression = "\${spring.rest.retry.maxDelay}"
    ),
    maxAttemptsExpression = "\${spring.rest.retry.maxAttempts}",

    )
class SifAbacPdpService(
    @Qualifier("sifAbacPdpKlient")
    private val sifAbacPdpKlient: RestTemplate
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(SifAbacPdpService::class.java)

        private val hendelseInnsendingUrl = "/ungdomsprogramveiledning"
    }

    fun ansattHarTilgang(input: UngdomsprogramTilgangskontrollInputDto): Boolean {
        val httpEntity = HttpEntity(input)
        val response = sifAbacPdpKlient.exchange(
            hendelseInnsendingUrl,
            HttpMethod.POST,
            httpEntity,
            Tilgangsbeslutning::class.java
        )
        return response.body!!.harTilgang()
    }

}

