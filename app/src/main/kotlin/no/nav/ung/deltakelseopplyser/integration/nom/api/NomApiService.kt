package no.nav.ung.deltakelseopplyser.integration.nom.api

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import no.nav.nom.generated.HentRessurser
import no.nav.nom.generated.hentressurser.OrgEnhet
import no.nav.nom.generated.hentressurser.Ressurs
import no.nav.ung.deltakelseopplyser.integration.nom.api.OrgEnhetUtils.harRelevantPeriode
import no.nav.ung.deltakelseopplyser.integration.nom.api.RessursOrgTilknytningUtils.harRelevantPeriode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

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

    data class RessursMedEnheter(val navIdent: String, val enheter: List<OrgEnhet>)
}
