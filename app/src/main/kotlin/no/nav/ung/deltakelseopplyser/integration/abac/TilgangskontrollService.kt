package no.nav.ung.deltakelseopplyser.integration.abac

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.security.token.support.client.core.context.JwtBearerTokenResolver
import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.sif.abac.kontrakt.abac.BeskyttetRessursActionAttributt
import no.nav.sif.abac.kontrakt.abac.dto.PersonerOperasjonDto
import no.nav.sif.abac.kontrakt.abac.dto.UngdomsprogramTilgangskontrollInputDto
import no.nav.sif.abac.kontrakt.abac.resultat.IkkeTilgangÅrsak
import no.nav.sif.abac.kontrakt.abac.resultat.Tilgangsbeslutning
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
    @Value("\${AZURE_APP_PRE_AUTHORIZED_APPS}") private val azureAppPreAuthorizedAppsString: String,
    @Value("\${ung.authorized-groups.programveileder}") private val programveilederGroupId: String,
    @Value("\${ung.authorized-groups.drift}") private val driftGroupId: String,
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(TilgangskontrollService::class.java)
    }

    val azureAppPreAuthorizedApps: List<PreauthorizedApp> by lazy {
        objectMapper.readValue<List<PreauthorizedApp>>(azureAppPreAuthorizedAppsString)
    }

    // Hent grupper fra token
    private fun hentBrukerGrupper(): List<String> {
        val jwt = hentTokenForInnloggetBruker()
        @Suppress("UNCHECKED_CAST")
        val groups = jwt.jwtClaimsSet.getClaim("groups") as? List<String> ?: emptyList()
        return groups
    }

    // Valider at bruker har minst én av forventede grupper
    private fun validerGruppemedlemskap(forventedeGruppeIds: List<String>) {
        val brukerGrupper = hentBrukerGrupper()
        val harGodkjentGruppe = forventedeGruppeIds.any { it in brukerGrupper }

        if (!harGodkjentGruppe) {
            logger.warn(
                "Bruker mangler godkjent gruppe. Forventet: {}, Bruker har: {}",
                forventedeGruppeIds,
                brukerGrupper
            )
            throw ErrorResponseException(
                HttpStatus.FORBIDDEN,
                ProblemDetail.forStatusAndDetail(
                    HttpStatus.FORBIDDEN,
                    "Bruker har ikke tilgang til denne ressursen (manglende gruppe)"
                ),
                null
            )
        }
    }

    fun krevAnsattTilgang(action: BeskyttetRessursActionAttributt, personIdenter: List<PersonIdent>) {
        // Automatisk valider programveileder-gruppe
        validerGruppemedlemskap(listOf(programveilederGroupId))

        val tilgangsbeslutning = ansattHarTilgang(action, personIdenter)
        if (!tilgangsbeslutning.harTilgang) {
            throw ErrorResponseException(
                HttpStatus.FORBIDDEN,
                ProblemDetail.forStatusAndDetail(
                    HttpStatus.FORBIDDEN,
                    tilgangsbeslutning.årsakerForIkkeTilgang.somTekst()
                ),
                null
            )
        }
    }

    fun ansattHarTilgang(
        action: BeskyttetRessursActionAttributt,
        personIdenter: List<PersonIdent>,
    ): Tilgangsbeslutning {
        return sifAbacPdpService.ansattHarTilgang(UngdomsprogramTilgangskontrollInputDto(action, personIdenter))
    }

    fun krevOboTilgangFraGodkjentEksternSystem(godkjenteApplikasjoner: List<String>, personerOperasjonDto: PersonerOperasjonDto) {
        if (erSystemBruker()) {
            throw ErrorResponseException(
                HttpStatus.FORBIDDEN,
                ProblemDetail.forStatusAndDetail(
                    HttpStatus.FORBIDDEN,
                    "Endepunktet aksepterer ikke systemtoken (maskin-til-maskin). Bruk OBO-token."
                ),
                null
            )
        }

        val jwt = hentTokenForInnloggetBruker()
        val azp = jwt.jwtTokenClaims.getStringClaim("azp")

        logger.info(
            "OBO-kall: azp '{}' azp_name '{}' godkjente applikasjoner '{}' godkjente clientID '{}'",
            azp,
            jwt.jwtTokenClaims.getStringClaim("azp_name"),
            godkjenteApplikasjoner,
            getGodkjenteClientIds(godkjenteApplikasjoner)
        )

        if (!erGodkjentApplikasjon(azp, godkjenteApplikasjoner)) {
            throw ErrorResponseException(
                HttpStatus.FORBIDDEN,
                ProblemDetail.forStatusAndDetail(
                    HttpStatus.FORBIDDEN,
                    "Kallet kommer fra et system som ikke har tilgang til dette endepunktet"
                ),
                null
            )
        }

        // Sjekk tilgang til personer
        val tilgangsbeslutning = sifAbacPdpService.sjekkTilgangTilPersonerForInnloggetBruker(personerOperasjonDto)

        if (!tilgangsbeslutning.harTilgang) {
            throw ErrorResponseException(
                HttpStatus.FORBIDDEN,
                ProblemDetail.forStatusAndDetail(
                    HttpStatus.FORBIDDEN,
                    tilgangsbeslutning.årsakerForIkkeTilgang.somTekst()
                ),
                null
            )
        }
    }

    fun krevSystemtilgang(godkjenteApplikasjoner: List<String> = listOf("ung-sak")) {
        val jwt = hentTokenForInnloggetBruker()
        val azp = jwt.jwtTokenClaims.getStringClaim("azp")

        logger.info(
            "Azp i token '{}' azpname '{}' godkjente applikasjoner '{}' godkjente clientID '{}'",
            azp,
            jwt.jwtTokenClaims.getStringClaim("azp_name"),
            godkjenteApplikasjoner,
            getGodkjenteClientIds(godkjenteApplikasjoner)
        )

        val harTilgang = erSystemBruker() && erGodkjentApplikasjon(azp, godkjenteApplikasjoner)
        if (!harTilgang) {
            throw ErrorResponseException(
                HttpStatus.FORBIDDEN,
                ProblemDetail.forStatusAndDetail(
                    HttpStatus.FORBIDDEN,
                    "Systemtjenesten er ikke tilgjengelig for innlogget bruker"
                ),
                null
            )
        }
    }

    private fun erGodkjentApplikasjon(azp: String, godkjenteApplikasjoner: List<String>): Boolean {
        val godkjenteClientIds = getGodkjenteClientIds(godkjenteApplikasjoner)
        return godkjenteClientIds.contains(azp)
    }

    private fun getGodkjenteClientIds(godkjenteApplikasjoner: List<String>): List<String> =
        godkjenteApplikasjoner.mapNotNull { clientIdForApplikasjon(it) }

    fun erSystemBruker(): Boolean {
        val jwt = hentTokenForInnloggetBruker()
        val azureIssuer = multiIssuerConfiguration.issuers["azure"]?.metadata?.issuer?.value
        val erAzureToken = azureIssuer != null && jwt.issuer == azureIssuer
        val erClientCredentials = jwt.jwtTokenClaims.getStringClaim("idtyp") == "app"
        return erAzureToken && erClientCredentials
    }


    fun clientIdForApplikasjon(appname: String): String? {
        val matches = azureAppPreAuthorizedApps
            .filter { it.name.substringAfterLast(":") == appname }

        return when (matches.size) {
            1 -> matches.first().clientId
            0 -> {
                logger.warn(
                    "Fant ingen pre-authorized app for '{}'. Tilgjengelige appnavn: {}",
                    appname,
                    azureAppPreAuthorizedApps.map { it.name }
                )
                null
            }

            else -> {
                throw IllegalArgumentException(
                    "Kan ikke unikt identifisere applikasjon $appname, har følgende kandidater: ${matches.map { it.name }}"
                )
            }
        }
    }

    fun krevDriftsTilgang(action: BeskyttetRessursActionAttributt) {
        // Automatisk valider drift-gruppe
        validerGruppemedlemskap(listOf(driftGroupId))

        val tilgangsbeslutning = harDriftstilgang(action)
        if (!tilgangsbeslutning.harTilgang) {
            throw ErrorResponseException(
                HttpStatus.FORBIDDEN,
                ProblemDetail.forStatusAndDetail(
                    HttpStatus.FORBIDDEN,
                    tilgangsbeslutning.årsakerForIkkeTilgang.somTekst()
                ),
                null
            )
        }
    }

    fun krevTilgangTilPersonerForInnloggetBruker(personerOperasjonDto: PersonerOperasjonDto) {
        val tilgangsbeslutning =
            sifAbacPdpService.sjekkTilgangTilPersonerForInnloggetBruker(personerOperasjonDto)

        if (!tilgangsbeslutning.harTilgang) {
            throw ErrorResponseException(
                HttpStatus.FORBIDDEN,
                ProblemDetail.forStatusAndDetail(
                    HttpStatus.FORBIDDEN,
                    tilgangsbeslutning.årsakerForIkkeTilgang.somTekst()
                ),
                null
            )
        }
    }

    fun harDriftstilgang(
        action: BeskyttetRessursActionAttributt,
    ): Tilgangsbeslutning {
        return sifAbacPdpService.harDriftstilgang(action)
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

    private fun MutableSet<IkkeTilgangÅrsak>.somTekst(): String {
        val årsaker = map {
            when (it) {
                IkkeTilgangÅrsak.HAR_IKKE_TILGANG_TIL_KODE6_PERSON -> "Ikke tilgang til kode6 person"
                IkkeTilgangÅrsak.HAR_IKKE_TILGANG_TIL_KODE7_PERSON -> "Ikke tilgang til kode7 person"
                IkkeTilgangÅrsak.HAR_IKKE_TILGANG_TIL_EGEN_ANSATT -> "Ikke tilgang til egen ansatt"
                IkkeTilgangÅrsak.HAR_IKKE_TILGANG_TIL_APPLIKASJONEN -> "Ikke tilgang til applikasjonen"
                IkkeTilgangÅrsak.ER_IKKE_VEILEDER_ELLER_SAKSBEHANDLER -> "Ikke veileder eller saksbehandler"
                IkkeTilgangÅrsak.ER_IKKE_SAKSBEHANDLER -> "Ikke saksbehandler"
                IkkeTilgangÅrsak.ER_IKKE_BESLUTTER -> "Ikke beslutter"
                IkkeTilgangÅrsak.ER_IKKE_OVERSTYRER -> "Ikke overstyrer"
                IkkeTilgangÅrsak.ER_IKKE_DRIFTER -> "Ikke drifter"
                IkkeTilgangÅrsak.ER_IKKE_UNGDSOMSPROGRAMVEILEDER -> "Ikke ungdomsprogramveileder"
                else -> "Ikke tilgang"
            }
        }

        return årsaker.joinToString(separator = "\n")
    }
}
