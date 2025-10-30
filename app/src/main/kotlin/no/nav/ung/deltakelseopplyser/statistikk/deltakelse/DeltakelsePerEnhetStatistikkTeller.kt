package no.nav.ung.deltakelseopplyser.statistikk.deltakelse

import no.nav.ung.deltakelseopplyser.historikk.AuditorAwareImpl.Companion.VEILEDER_SUFFIX
import no.nav.ung.deltakelseopplyser.integration.nom.api.domene.OrgEnhetMedPeriode
import no.nav.ung.deltakelseopplyser.integration.nom.api.domene.RessursMedAlleTilknytninger
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
        ressurserMedTilknytninger: List<RessursMedAlleTilknytninger>,
    ): DeltakelsePerEnhetResultat {
        val ressursLookup = ressurserMedTilknytninger.associateBy { it.navIdent }
        val veiledereMedFlereEnheter = mutableMapOf<String, List<OrgEnhetMedPeriode>>()

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
        ressursLookup: Map<String, RessursMedAlleTilknytninger>,
        alleRessurser: List<RessursMedAlleTilknytninger>,
        veiledereMedFlereEnheter: MutableMap<String, List<OrgEnhetMedPeriode>>,
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
                    val valgtEnhet = velgMestPopulæreEnhet(
                        gyldigeEnheter,
                        alleRessurser,
                        navIdent,
                        deltakelse.opprettetDato,
                        veiledereMedFlereEnheter
                    )
                    valgtEnhet.navn
                }
            }
        } ?: run {
            logger.warn("Fant ingen ressurs for NAV-ident $navIdent")
            null
        }
    }

    private fun finnGyldigeEnheter(
        ressurs: RessursMedAlleTilknytninger,
        opprettetDato: LocalDate,
    ): List<OrgEnhetMedPeriode> {
        // Først: Prøv å finne enheter som er gyldige på eksakt tidspunkt
        val eksaktGyldigeEnheter = ressurs.orgTilknytninger
            .filter { it.erGyldigPåTidspunkt(opprettetDato) }
            .map { it.orgEnhet }
            .filter { it.erGyldigPåTidspunkt(opprettetDato) }
            .distinctBy { "${it.id}-${it.navn}" }

        if (eksaktGyldigeEnheter.isNotEmpty()) {
            return eksaktGyldigeEnheter
        }

        // Fallback: Finn siste gyldige enhet innen toleranseperiode (30 dager)
        val toleranseDager = 30L

        val sisteGyldigeEnhet = ressurs.orgTilknytninger
            .filter { tilknytning ->
                // Tilknytningen må ha startet før opprettelsesdato
                !tilknytning.gyldigFom.isAfter(opprettetDato) &&
                        // Enheten må ha sluttet nylig (innen toleranseperiode)
                        tilknytning.gyldigTom != null &&
                        !tilknytning.gyldigTom.isBefore(opprettetDato.minusDays(toleranseDager))
            }
            .sortedByDescending { it.gyldigTom }
            .take(1)
            .map { it.orgEnhet }

        if (sisteGyldigeEnhet.isNotEmpty()) {
            logger.info(
                "Bruker fallback: Fant ingen gyldig enhet for NAV-ident ${ressurs.navIdent} på $opprettetDato, " +
                        "bruker siste gyldige enhet: ${sisteGyldigeEnhet.first().navn} " +
                        "(gyldigTom: ${ressurs.orgTilknytninger.first { it.orgEnhet.id == sisteGyldigeEnhet.first().id }.gyldigTom})"
            )
            return sisteGyldigeEnhet
        }

        logger.warn("Fant ingen gyldig enhet for NAV-ident ${ressurs.navIdent} på tidspunkt $opprettetDato, heller ikke i fallback. Prøv å øke toleranseDager.")
        return emptyList()
    }

    private fun velgMestPopulæreEnhet(
        gyldigeEnheter: List<OrgEnhetMedPeriode>,
        alleRessurser: List<RessursMedAlleTilknytninger>,
        navIdent: String,
        opprettetDato: LocalDate,
        veiledereMedFlereEnheter: MutableMap<String, List<OrgEnhetMedPeriode>>,
    ): OrgEnhetMedPeriode {
        val enhetPopularitet = beregnEnhetPopularitet(alleRessurser)
        val mestPopulæreEnhet = gyldigeEnheter.maxByOrNull {
            enhetPopularitet["${it.id}-${it.navn}"] ?: 0
        } ?: gyldigeEnheter.first()

        loggFlereEnheterValg(gyldigeEnheter, enhetPopularitet, mestPopulæreEnhet, opprettetDato)
        veiledereMedFlereEnheter[navIdent] = gyldigeEnheter

        return mestPopulæreEnhet
    }

    private fun beregnEnhetPopularitet(ressurserMedTilknytninger: List<RessursMedAlleTilknytninger>): Map<String, Int> =
        ressurserMedTilknytninger
            .flatMap { it.orgTilknytninger }
            .map { it.orgEnhet }
            .groupingBy { "${it.id}-${it.navn}" }
            .eachCount()

    private fun loggFlereEnheterValg(
        gyldigeEnheter: List<OrgEnhetMedPeriode>,
        enhetPopularitet: Map<String, Int>,
        valgtEnhet: OrgEnhetMedPeriode,
        opprettetDato: LocalDate,
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
        veiledereMedFlereEnheter: Map<String, List<OrgEnhetMedPeriode>>,
        ressurserMedTilknytninger: List<RessursMedAlleTilknytninger>,
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
