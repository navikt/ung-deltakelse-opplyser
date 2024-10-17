package no.nav.ung.deltakelseopplyser.integration.pdl

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import no.nav.pdl.generated.HentIdent
import no.nav.pdl.generated.enums.IdentGruppe
import no.nav.pdl.generated.hentident.IdentInformasjon
import no.nav.pdl.generated.hentident.Identliste
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PdlService(
    private val pdlClient: GraphQLWebClient,
    private val objectMapper: ObjectMapper,
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(PdlService::class.java)
    }

    fun hentIdenter(ident: String, historisk: Boolean = false, identGruppe: IdentGruppe = IdentGruppe.FOLKEREGISTERIDENT): Identliste = runBlocking {
        val response = pdlClient.execute(HentIdent(HentIdent.Variables(ident, listOf(identGruppe))))

        if (!response.extensions.isNullOrEmpty()) logger.info("PDL response extensions: ${response.extensions}")

        when {
            !response.errors.isNullOrEmpty() -> {
                val errorSomJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response.errors)
                logger.error("Feil ved henting av identListe. Årsak: {}", errorSomJson)
                throw IllegalStateException("Feil ved henting av identListe.")
            }

            response.data!!.hentIdenter != null -> response.data!!.hentIdenter!!.identer
                .filter { historisk || !it.historisk }
                .let { Identliste(it) }

            else -> {
                error("Feil ved henting av person.")
            }
        }
    }

    fun hentFolkeregisteridenter(ident: String): List<IdentInformasjon> {
        val identliste = hentIdenter(ident = ident)
        return identliste.identer.filter { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT }
    }

    fun hentAktørIder(ident: String, historisk: Boolean = false): List<IdentInformasjon> {
        val identliste = hentIdenter(ident = ident, identGruppe = IdentGruppe.AKTORID, historisk = historisk)
        return runCatching {
            identliste.identer.filter { it.gruppe == IdentGruppe.AKTORID }
        }
            .getOrElse {
                logger.error("Fant ingen aktørIder. Historisk=$historisk")
                throw IllegalStateException("Fant ingen historiske aktørId. Historisk=$historisk")
            }
    }
}
