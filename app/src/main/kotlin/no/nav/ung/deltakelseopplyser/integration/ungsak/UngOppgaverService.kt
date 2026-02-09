package no.nav.ung.deltakelseopplyser.integration.ungsak

import no.nav.ung.sak.kontrakt.oppgaver.OpprettSøkYtelseOppgaveDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Recover
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import java.util.*

@Service
@Retryable(
    noRetryFor = [UngSakException::class, HttpClientErrorException.Unauthorized::class, HttpClientErrorException.Forbidden::class, ResourceAccessException::class],
    backoff = Backoff(
        delayExpression = "\${spring.rest.retry.initialDelay}",
        multiplierExpression = "\${spring.rest.retry.multiplier}",
        maxDelayExpression = "\${spring.rest.retry.maxDelay}"
    ),
    maxAttemptsExpression = "\${spring.rest.retry.maxAttempts}",

    )
class UngOppgaverService(
    @Qualifier("ungOppgaverKlient")
    private val ungOppgaverKlient: RestTemplate,
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(UngOppgaverService::class.java)

        private const val opprettSøkYtelseUrl = "/api/oppgave/opprett/sok-ytelse"
        private const val lukkOppgaveUrl = "/api/oppgave/{oppgaveReferanse}/lukk"
        private const val apneOppgaveUrl = "/api/oppgave/{oppgaveReferanse}/apne"
        private const val losOppgaveUrl = "/api/oppgave/{oppgaveReferanse}/los"
    }

    fun opprettSøkYtelseOppgave(opprettOppgave: OpprettSøkYtelseOppgaveDto): Boolean {
        val httpEntity = HttpEntity(opprettOppgave)
        val response = ungOppgaverKlient.exchange(
            opprettSøkYtelseUrl,
            HttpMethod.POST,
            httpEntity,
            Unit::class.java
        )
        return response.statusCode == HttpStatus.OK
    }

    @Recover
    private fun opprettSøkYtelseOppgave(
        exception: HttpClientErrorException,
        opprettOppgave: OpprettSøkYtelseOppgaveDto,
    ): Boolean {
        logger.error("Fikk en HttpClientErrorException når man kalte opprettSøkYtelseOppgave tjeneste i ung-sak. Error response = '${exception.responseBodyAsString}'")
        return false
    }

    @Recover
    private fun opprettSøkYtelseOppgave(
        exception: HttpServerErrorException,
        opprettOppgave: OpprettSøkYtelseOppgaveDto,
    ): Boolean {
        logger.error("Fikk en HttpServerErrorException når man kalte opprettSøkYtelseOppgave tjeneste i ung-sak.")
        return false
    }

    @Recover
    private fun opprettSøkYtelseOppgave(
        exception: ResourceAccessException,
        opprettOppgave: OpprettSøkYtelseOppgaveDto,
    ): Boolean {
        logger.error("Fikk en ResourceAccessException når man kalte opprettSøkYtelseOppgave tjeneste i ung-sak.")
        return false
    }

    fun lukkOppgave(oppgaveReferanse: UUID): Boolean {
        return try {
            val response = ungOppgaverKlient.exchange(
                lukkOppgaveUrl,
                HttpMethod.PUT,
                null,
                Unit::class.java,
                oppgaveReferanse
            )
            response.statusCode == HttpStatus.OK
        } catch (e: HttpServerErrorException) {
            if (e.statusCode == HttpStatus.INTERNAL_SERVER_ERROR &&
                e.responseBodyAsString.contains("Fant ikke oppgave med oppgavereferanse:")) {
                logger.warn("Ung-sak fant ikke oppgave med oppgavereferanse: $oppgaveReferanse. Dette er forventet for eldre oppgaver.")
                true // Return true to indicate we handled this gracefully
            } else {
                throw e
            }
        }
    }

    fun åpneOppgave(oppgaveReferanse: UUID): Boolean {
        return try {
            val response = ungOppgaverKlient.exchange(
                apneOppgaveUrl,
                HttpMethod.PUT,
                null,
                Unit::class.java,
                oppgaveReferanse
            )
            response.statusCode == HttpStatus.OK
        } catch (e: HttpServerErrorException) {
            if (e.statusCode == HttpStatus.INTERNAL_SERVER_ERROR &&
                e.responseBodyAsString.contains("Fant ikke oppgave med oppgavereferanse:")) {
                logger.warn("Ung-sak fant ikke oppgave med oppgavereferanse: $oppgaveReferanse. Dette er forventet for eldre oppgaver.")
                true // Return true to indicate we handled this gracefully
            } else {
                throw e
            }
        }
    }

    fun løsOppgave(oppgaveReferanse: UUID): Boolean {
        return try {
            val response = ungOppgaverKlient.exchange(
                losOppgaveUrl,
                HttpMethod.PUT,
                null,
                Unit::class.java,
                oppgaveReferanse
            )
            response.statusCode == HttpStatus.OK
        } catch (e: HttpServerErrorException) {
            if (e.statusCode == HttpStatus.INTERNAL_SERVER_ERROR &&
                e.responseBodyAsString.contains("Fant ikke oppgave med oppgavereferanse:")) {
                logger.warn("Ung-sak fant ikke oppgave med oppgavereferanse: $oppgaveReferanse. Dette er forventet for eldre oppgaver.")
                true // Return true to indicate we handled this gracefully
            } else {
                throw e
            }
        }
    }
}
