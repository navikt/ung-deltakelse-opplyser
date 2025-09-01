package no.nav.ung.deltakelseopplyser.integration.nom.api

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import no.nav.nom.generated.HentRessurser
import no.nav.nom.generated.hentressurser.OrgEnhet
import no.nav.nom.generated.hentressurser.Ressurs
import no.nav.ung.deltakelseopplyser.integration.nom.api.OrgEnhetUtils.erGyldigPåTidspunkt
import no.nav.ung.deltakelseopplyser.integration.nom.api.OrgEnhetUtils.harRelevantPeriode
import no.nav.ung.deltakelseopplyser.integration.nom.api.RessursOrgTilknytningUtils.erGyldigPåTidspunkt
import no.nav.ung.deltakelseopplyser.integration.nom.api.RessursOrgTilknytningUtils.harRelevantPeriode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class NomApiService(
    private val nomApiClient: GraphQLWebClient,
    private val objectMapper: ObjectMapper,
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(NomApiService::class.java)
    }

    private fun hentRessurser(navIdenter: Set<String>): List<Ressurs> = runBlocking {
        logger.info("Henter resssurs info for {} navIdenter", navIdenter.size)
        val response = nomApiClient.execute(HentRessurser(HentRessurser.Variables(navIdenter = navIdenter.toList())))

        if (!response.extensions.isNullOrEmpty()) logger.info("Nom-API response extensions: ${response.extensions}")

        val ressurser = when {
            !response.errors.isNullOrEmpty() -> {
                val errorSomJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response.errors)
                logger.error("Feil ved henting av ressurser. Årsak: {}", errorSomJson)
                throw IllegalStateException("Feil ved henting av ressurser.")
            }

            response.data!!.ressurser.isNotEmpty() -> response.data!!.ressurser.mapNotNull { it.ressurs }

            else -> {
                error("Feil ved henting av person.")
            }
        }
        if (ressurser.size != navIdenter.size) {
            logger.warn("Antall hentede ressurser (${ressurser.size}) er ikke lik antall forespurte navIdenter (${navIdenter.size}).")
        }
        logger.info("Hentet ${ressurser.size} ressurser fra Nom-API")
        ressurser
    }

    fun hentResursserMedEnheter(navIdenter: Set<String>): List<RessursMedEnheter> {
        val ressursMedEnheter = hentRessurser(navIdenter)
            .map { ressurs ->
                val relevanteEnheter = ressurs.orgTilknytning
                    .filter { it.harRelevantPeriode() }
                    .map { orgTilknytning -> orgTilknytning.orgEnhet }
                    .filter { orgEnhet -> orgEnhet.harRelevantPeriode() }

                RessursMedEnheter(
                    navIdent = ressurs.navident,
                    enheter = relevanteEnheter
                )
            }

        logger.info("Fant {} unike enheter for {} forespurte navIdenter", ressursMedEnheter.size, navIdenter.size)
        return ressursMedEnheter
    }

    /**
     * Hent ressurser med enheter som var gyldige på spesifikke tidspunkter.
     * For hver (navIdent, tidspunkt) kombinasjon, returner ressursen med enheter som var gyldige på det tidspunktet.
     * Hvis en ressurs ikke finnes for en navIdent, eller ingen enheter var gyldige på det tidspunktet, ekskluderes den fra resultatet.
     *
     * @param navIdenterMedTidspunkt Set av NavIdentOgTidspunkt, hvor hver inneholder en navIdent og et tidspunkt (LocalDate).
     * @return Liste av RessursMedEnheter, hvor hver ressurs inneholder navIdent og en liste av OrgEnhet som var gyldige på det spesifikke tidspunktet.
     */
    fun hentResursserMedEnheterForTidspunkter(navIdenterMedTidspunkt: Set<NavIdentOgTidspunkt>): List<RessursMedEnheter> {
        val alleNavIdenter = navIdenterMedTidspunkt.map { it.navIdent }.toSet()

        val ressurser = hentRessurser(alleNavIdenter)
        val ressursLookup = ressurser.associateBy { it.navident }

        return navIdenterMedTidspunkt.mapNotNull { navIdentOgTidspunkt ->
            val navIdent = navIdentOgTidspunkt.navIdent
            val tidspunkt = navIdentOgTidspunkt.tidspunkt

            val ressurs = ressursLookup[navIdent]
            if (ressurs != null) {
                val relevanteEnheter = ressurs.orgTilknytning
                    .filter { it.erGyldigPåTidspunkt(tidspunkt) }
                    .map { orgTilknytning -> orgTilknytning.orgEnhet }
                    .filter { orgEnhet -> orgEnhet.erGyldigPåTidspunkt(tidspunkt) }

                val ressursMedEnheter = RessursMedEnheter(
                    navIdent = ressurs.navident,
                    enheter = relevanteEnheter.distinctBy { it.id }
                )

                ressursMedEnheter
            } else {
                null
            }
        }
    }

    data class NavIdentOgTidspunkt(val navIdent: String, val tidspunkt: LocalDate)

    data class RessursMedEnheter(val navIdent: String, val enheter: List<OrgEnhet>)
}
