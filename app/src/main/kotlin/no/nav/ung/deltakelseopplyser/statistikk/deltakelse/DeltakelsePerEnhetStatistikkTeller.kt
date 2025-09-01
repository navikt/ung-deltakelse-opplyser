package no.nav.ung.deltakelseopplyser.statistikk.deltakelse

import no.nav.ung.deltakelseopplyser.historikk.AuditorAwareImpl.Companion.VEILEDER_SUFFIX
import no.nav.ung.deltakelseopplyser.integration.nom.api.NomApiService
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

data class DeltakelseInput(
    val id: UUID,
    val opprettetAv: String,
    val opprettetDato: LocalDate,
) {
    fun navIdent() = opprettetAv.removeSuffix(VEILEDER_SUFFIX).trim()
}

data class DeltakelsePerEnhetResultat(
    val deltakelserPerEnhet: Map<String, Int>,
    val diagnostikk: Map<Any, Any?> = emptyMap(),
)

class DeltakelsePerEnhetStatistikkTeller {
    private companion object {
        private val logger = LoggerFactory.getLogger(DeltakelsePerEnhetStatistikkTeller::class.java)
    }

    fun tellAntallDeltakelserPerEnhet(
        deltakelser: List<DeltakelseInput>,
        ressurserMedTilknytninger: List<NomApiService.RessursMedAlleTilknytninger>,
    ): DeltakelsePerEnhetResultat {
        val ressursLookup = ressurserMedTilknytninger.associateBy { it.navIdent }
        val veiledereMedFlereEnheter = mutableMapOf<String, List<NomApiService.OrgEnhetMedPeriode>>()

        val deltakelserPerEnhet = deltakelser
            .mapNotNull { deltakelse ->
                finnEnhetForDeltakelse(deltakelse, ressursLookup, ressurserMedTilknytninger, veiledereMedFlereEnheter)
            }
            .groupingBy { it }
            .eachCount()

        return opprettResultat(deltakelser, deltakelserPerEnhet, veiledereMedFlereEnheter, ressurserMedTilknytninger)
    }

    private fun finnEnhetForDeltakelse(
        deltakelse: DeltakelseInput,
        ressursLookup: Map<String, NomApiService.RessursMedAlleTilknytninger>,
        alleRessurser: List<NomApiService.RessursMedAlleTilknytninger>,
        veiledereMedFlereEnheter: MutableMap<String, List<NomApiService.OrgEnhetMedPeriode>>
    ): String? {
        val navIdent = deltakelse.navIdent()

        return ressursLookup[navIdent]?.let { ressurs ->
            val gyldigeEnheter = finnGyldigeEnheter(ressurs, deltakelse.opprettetDato)

            when {
                gyldigeEnheter.isEmpty() -> {
                    logger.warn("Fant ingen gyldig enhet for NAV-ident $navIdent på tidspunkt ${deltakelse.opprettetDato}")
                    null
                }
                gyldigeEnheter.size == 1 -> gyldigeEnheter.first().navn
                else -> {
                    val valgtEnhet = velgMestPopulæreEnhet(gyldigeEnheter, alleRessurser, navIdent, deltakelse.opprettetDato, veiledereMedFlereEnheter)
                    valgtEnhet.navn
                }
            }
        } ?: run {
            logger.warn("Fant ingen ressurs for NAV-ident $navIdent")
            null
        }
    }

    private fun finnGyldigeEnheter(
        ressurs: NomApiService.RessursMedAlleTilknytninger,
        opprettetDato: LocalDate
    ): List<NomApiService.OrgEnhetMedPeriode> {
        return ressurs.orgTilknytninger
            .filter { it.erGyldigPåTidspunkt(opprettetDato) }
            .map { it.orgEnhet }
            .filter { it.erGyldigPåTidspunkt(opprettetDato) }
            .distinctBy { "${it.id}-${it.navn}" }
    }

    private fun velgMestPopulæreEnhet(
        gyldigeEnheter: List<NomApiService.OrgEnhetMedPeriode>,
        alleRessurser: List<NomApiService.RessursMedAlleTilknytninger>,
        navIdent: String,
        opprettetDato: LocalDate,
        veiledereMedFlereEnheter: MutableMap<String, List<NomApiService.OrgEnhetMedPeriode>>
    ): NomApiService.OrgEnhetMedPeriode {
        val enhetPopularitet = beregnEnhetPopularitet(alleRessurser)
        val mestPopulæreEnhet = gyldigeEnheter.maxByOrNull {
            enhetPopularitet["${it.id}-${it.navn}"] ?: 0
        } ?: gyldigeEnheter.first()

        loggFlereEnheterValg(gyldigeEnheter, enhetPopularitet, mestPopulæreEnhet, opprettetDato)
        veiledereMedFlereEnheter[navIdent] = gyldigeEnheter

        return mestPopulæreEnhet
    }

    private fun beregnEnhetPopularitet(ressurserMedTilknytninger: List<NomApiService.RessursMedAlleTilknytninger>): Map<String, Int> =
        ressurserMedTilknytninger
            .flatMap { it.orgTilknytninger }
            .map { it.orgEnhet }
            .groupingBy { "${it.id}-${it.navn}" }
            .eachCount()

    private fun loggFlereEnheterValg(
        gyldigeEnheter: List<NomApiService.OrgEnhetMedPeriode>,
        enhetPopularitet: Map<String, Int>,
        valgtEnhet: NomApiService.OrgEnhetMedPeriode,
        opprettetDato: LocalDate
    ) {
        val enhetInfo = gyldigeEnheter.joinToString { enhet ->
            val popularitet = enhetPopularitet["${enhet.id}-${enhet.navn}"] ?: 0
            "${enhet.id}-${enhet.navn} (popularitet: $popularitet)"
        }

        logger.warn(
            "NAV-ident hadde ${gyldigeEnheter.size} enheter på tidspunkt $opprettetDato [$enhetInfo], " +
            "valgte den mest populære: ${valgtEnhet.id}-${valgtEnhet.navn}"
        )
    }

    private fun opprettResultat(
        deltakelser: List<DeltakelseInput>,
        deltakelserPerEnhet: Map<String, Int>,
        veiledereMedFlereEnheter: Map<String, List<NomApiService.OrgEnhetMedPeriode>>,
        ressurserMedTilknytninger: List<NomApiService.RessursMedAlleTilknytninger>
    ): DeltakelsePerEnhetResultat {
        val antallUnikeNavIdenter = deltakelser
            .map { it.navIdent() }
            .toSet()
            .size

        logger.info("Fant $antallUnikeNavIdenter unike NAV-identer fra ${deltakelser.size} deltakelser")

        return DeltakelsePerEnhetResultat(
            deltakelserPerEnhet = deltakelserPerEnhet,
            diagnostikk = mapOf(
                "enhetPopularitet" to beregnEnhetPopularitet(ressurserMedTilknytninger),
                "veiledereMedFlereEnheter" to veiledereMedFlereEnheter,
                "totalAntallDeltakelser" to deltakelser.size,
                "antallUnikeNavIdenter" to antallUnikeNavIdenter
            )
        )
    }
}
