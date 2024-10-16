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

    private fun hentIdenter(ident: String, historisk: Boolean = false, identGruppe: IdentGruppe = IdentGruppe.FOLKEREGISTERIDENT): Identliste = runBlocking {
        val response = pdlClient.execute(HentIdent(HentIdent.Variables(ident, listOf(identGruppe))))

        if (!response.extensions.isNullOrEmpty()) logger.info("PDL response extensions: ${response.extensions}")

        when {
            !response.errors.isNullOrEmpty() -> {
                val errorSomJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response.errors)
                logger.error("Feil ved henting av identListe. Årsak: {}", errorSomJson)
                throw IllegalStateException("Feil ved henting av identListe.")
            }

            response.data!!.hentIdenter != null -> response.data!!.hentIdenter!!.identer
                .filter { it.historisk == historisk }
                .let { Identliste(it) }

            else -> {
                error("Feil ved henting av person.")
            }
        }
    }

    fun hentFolkeregisterident(ident: String): String {
        val identliste = hentIdenter(ident = ident)
        return runCatching {
            identliste.identer
                .filterNot { it.historisk }
                .first { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT }.ident
        }
            .getOrElse {
                logger.error("Fant ingen folkeregisterident.")
                throw IllegalStateException("Fant ingen folkeregisterident.")
            }
    }

    fun hentAktørId(ident: String): String {
        val identliste = hentIdenter(ident = ident, identGruppe = IdentGruppe.AKTORID)
        return runCatching {
            identliste.identer.first { it.gruppe == IdentGruppe.AKTORID }.ident
        }
            .getOrElse {
                logger.error("Fant ingen aktørId.")
                throw IllegalStateException("Fant ingen aktørId.")
            }
    }

    fun hentHistoriskeAktørIder(ident: String): List<IdentInformasjon> {
        val identliste = hentIdenter(ident = ident, identGruppe = IdentGruppe.AKTORID, historisk = true)
        return runCatching {
            identliste.identer.filter { it.gruppe == IdentGruppe.AKTORID }
        }
            .getOrElse {
                logger.error("Fant ingen historiske aktørIder.")
                throw IllegalStateException("Fant ingen aktørId.")
            }
    }
}
