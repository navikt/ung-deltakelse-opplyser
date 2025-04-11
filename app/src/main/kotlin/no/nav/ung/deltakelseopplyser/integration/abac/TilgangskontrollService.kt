package no.nav.ung.deltakelseopplyser.integration.abac

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.security.token.support.client.core.context.JwtBearerTokenResolver
import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.sif.abac.kontrakt.abac.BeskyttetRessursActionAttributt
import no.nav.sif.abac.kontrakt.abac.dto.UngdomsprogramTilgangskontrollInputDto
import no.nav.sif.abac.kontrakt.person.PersonIdent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.stereotype.Service
import org.springframework.web.ErrorResponseException

@Service
class TilgangskontrollService(
    private val objectMapper: ObjectMapper,
    private val sifAbacPdpService: SifAbacPdpService,
    private val tokenResolver: JwtBearerTokenResolver,
    private val multiIssuerConfiguration: MultiIssuerConfiguration,
    @Value("\${ABAC_ENABLED}") private val abacEnabled: String,
    @Value("\${azure.app.pre.authorized.apps") private val azureAppPreAuthorizedAppsString: String
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(TilgangskontrollService::class.java)
    }

    val azureAppPreAuthorizedApps: List<PreauthorizedApp> by lazy {
        objectMapper.readValue<List<PreauthorizedApp>>(azureAppPreAuthorizedAppsString)
    }

    fun krevAnsattTilgang(action: BeskyttetRessursActionAttributt, personIdenter: List<PersonIdent>) {
        if (!ansattHarTilgang(action, personIdenter)) {
            throw ErrorResponseException(
                HttpStatus.FORBIDDEN,
                ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Har ikke tilgang"),
                null
            )
        }
    }

    fun ansattHarTilgang(action: BeskyttetRessursActionAttributt, personIdenter: List<PersonIdent>): Boolean {
        if (abacEnabled == "false") {
            logger.info("Tilgangskontroll er disabled")
            return true;
        }
        return sifAbacPdpService.ansattHarTilgang(UngdomsprogramTilgangskontrollInputDto(action, personIdenter))
    }

    fun krevSystemtilgang(godkjenteApplikasjoner: List<String> = listOf("ung-sak")) {
        try {
            val jwt = hentTokenForInnloggetBruker()
            val erAzureToken = jwt.issuer == multiIssuerConfiguration.issuers["azure"]!!.metadata.issuer.value
            val erClientCredentials = jwt.jwtTokenClaims.getStringClaim("idtyp") == "app"
            val azp = jwt.jwtTokenClaims.getStringClaim("azp")
            val godkjenteClidentIds = godkjenteApplikasjoner.map { clientIdForApplikasjon(it) }
            val erGodkjentApplikasjon = godkjenteClidentIds.contains(azp)
            logger.info("Azp i token '{}' azpname '{}' godkjente applikasjoner '{}' godkjente clientID '{}'", azp, jwt.jwtTokenClaims.getStringClaim("azp_name"), godkjenteApplikasjoner, godkjenteClidentIds)
            val tilgang = erAzureToken && erClientCredentials && erGodkjentApplikasjon
            if (abacEnabled == "false") {
                logger.info("Tilgangskontroll er disabled")
                return;
            }
            if (!tilgang) {
                throw ErrorResponseException(
                    HttpStatus.FORBIDDEN,
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.FORBIDDEN,
                        "Systemtjenesten er ikke tilgjengelig for innlogget bruker"
                    ),
                    null
                )
            }
        } catch (e: ErrorResponseException) {
            throw e
        } catch (e: Exception) {
            if (abacEnabled == "false") {
                logger.warn("Feil i tilgangskontroll, tilgangskontroll er disablet så ignorerer feil", e)
            } else {
                throw e
            }
        }
    }

    fun clientIdForApplikasjon(appname: String): String {
        val matches = azureAppPreAuthorizedApps
            .filter { it.name.substringAfterLast(":") == appname }
        if (matches.size == 1) {
            return matches.first().clientId
        } else {
            throw IllegalArgumentException("Kan ikke unikt identifisere applikasjon " + appname + ", har følgende kandidater: " + matches.map { it.name })
        }
    }

    private fun hentTokenForInnloggetBruker(): JwtToken {
        val jwtAsString: String = tokenResolver.token() ?: throw ErrorResponseException(
            HttpStatus.UNAUTHORIZED,
            ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Fant ikke JWT"),
            null
        )
        return jwtToken(jwtAsString)
    }

    private fun jwtToken(jwtAsString: String): JwtToken {
        val jwt = JwtToken(jwtAsString)
        return jwt
    }

    data class PreauthorizedApp(val name: String, val clientId: String)
}

