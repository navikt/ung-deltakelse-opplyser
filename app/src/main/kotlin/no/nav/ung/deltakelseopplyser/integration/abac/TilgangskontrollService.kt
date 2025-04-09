package no.nav.ung.deltakelseopplyser.integration.abac

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.k9.felles.konfigurasjon.env.Environment
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
    @Value("\${abac.enabled}") private val abacEnabled: String,
    @Value("\${azure.app.pre.authorized.apps") private val azureAppPreAuthorizedAppsString: String
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(TilgangskontrollService::class.java)
    }

    val ungSakClientId: String by lazy {
        val ungSakApplikasjon = objectMapper.readValue<List<PreauthorizedApp>>(azureAppPreAuthorizedAppsString)
            .filter { it.clientId.substringAfterLast(":") == "ung-sak" }
        if (ungSakApplikasjon.size == 1) {
            return@lazy ungSakApplikasjon.first().clientId
        } else {
            throw IllegalArgumentException("Kan ikke unikt identifisere applikasjon ung-sak, har følgende kandidater: " + ungSakApplikasjon.map { PreauthorizedApp::name })
        }
    }

    fun krevSystemtilgang() {
        try {
            logger.info("Tilgjengelige issuers: {}", multiIssuerConfiguration.issuers.keys)
            val azureIssuer = multiIssuerConfiguration.issuers["azure"]!!.metadata.issuer.value
            logger.info("Utledet azure issuer til å være '{}'", azureIssuer)
            val jwtAsString: String = tokenResolver.token() ?: throw ErrorResponseException(
                HttpStatus.UNAUTHORIZED,
                ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Fant ikke JWT"),
                null
            )
            val jwt = JwtToken(jwtAsString)
            val erAzureToken = jwt.issuer == azureIssuer
            logger.info("Issuer i token '{}' erAzureToken {}", jwt.issuer, erAzureToken)
            val idtype = jwt.jwtTokenClaims.getStringClaim("idtyp")
            val erClientCredentials = idtype == "app"
            logger.info("Er client credentials {} pga idtyp '{}'", erClientCredentials, idtype)
            val azp = jwt.jwtTokenClaims.getStringClaim("azp")
            val erUngSak = ungSakClientId == azp
            logger.info(
                "Azp name '{}' azp '{}' ungsak-clientId '{}'",
                jwt.jwtTokenClaims.getStringClaim("azp_name"),
                azp,
                ungSakClientId
            )

            val tilgang = erAzureToken && erClientCredentials && erUngSak
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

    data class PreauthorizedApp(val name: String, val clientId: String)
}

