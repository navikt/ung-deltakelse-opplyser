package no.nav.ung.deltakelseopplyser.statistikk.deltakelse

import no.nav.ung.deltakelseopplyser.historikk.AuditorAwareImpl.Companion.VEILEDER_SUFFIX
import no.nav.ung.deltakelseopplyser.integration.nom.api.domene.OrgEnhetMedPeriode
import no.nav.ung.deltakelseopplyser.integration.nom.api.domene.RessursMedAlleTilknytninger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Input til statistikkberegning. Representerer én deltakelse med info om hvem som opprettet den og når.
 * [opprettetAv] inneholder veilederens ident med suffiks, f.eks. "H111111 (veileder)".
 */
data class DeltakelseInput(
    val id: UUID,
    val opprettetAv: String,
    val opprettetDato: LocalDate,
) {
    /** Ekstraher ren NAV-ident ved å fjerne " (veileder)"-suffiks. */
    fun navIdent() = opprettetAv.removeSuffix(VEILEDER_SUFFIX).trim()
}

data class DeltakelsePerEnhetResultat(
    val deltakelserPerEnhet: Map<String, Int>,
    val diagnostikk: Map<Any, Any?> = emptyMap(),
)

/**
 * Teller antall deltakelser per NAV-enhet.
 *
 * Tellingen fungerer slik:
 * 1. Hver deltakelse mappes til enheten veilederen tilhørte på opprettelsestidspunktet (via NOM).
 * 2. Hvis veilederen hadde flere gyldige enheter på datoen, velges den mest populære (flest veiledere totalt).
 * 3. Hvis ingen enhet er gyldig på eksakt dato, brukes nærmeste tilknytning som fallback.
 * 4. Deltakelser som ikke kan mappes til noen enhet, telles under [ENHET_SIKKERHETSNETT]
 *    slik at totalsummen alltid stemmer med antall input-deltakelser.
 */
class DeltakelsePerEnhetStatistikkTeller {
    companion object {
        private val logger = LoggerFactory.getLogger(DeltakelsePerEnhetStatistikkTeller::class.java)
        const val ENHET_SIKKERHETSNETT = "Enhet sikkerhetsnett"
    }

    /**
     * Hovedmetode: Teller antall deltakelser gruppert per enhetsnavn.
     * Returnerer et resultat der summen av alle verdier alltid er lik [deltakelser].size.
     */
    fun tellAntallDeltakelserPerEnhet(
        deltakelser: List<DeltakelseInput>,
        ressurserMedTilknytninger: List<RessursMedAlleTilknytninger>,
    ): DeltakelsePerEnhetResultat {
        // Oppslag-map for rask lookup av NOM-ressurs per NAV-ident
        val ressursLookup = ressurserMedTilknytninger.associateBy { it.navIdent }
        val veiledereMedFlereEnheter = mutableMapOf<String, List<OrgEnhetMedPeriode>>()
        val deltakelserUtenEnhet = mutableListOf<DeltakelseInput>()

        // Map hver deltakelse til et enhetsnavn. Hvis ingen enhet finnes, brukes ENHET_SIKKERHETSNETT
        // som fallback slik at ingen deltakelser går tapt fra tellingen.
        val deltakelserPerEnhet = deltakelser
            .map { deltakelse ->
                finnEnhetForDeltakelse(deltakelse, ressursLookup, ressurserMedTilknytninger, veiledereMedFlereEnheter)
                    ?: ENHET_SIKKERHETSNETT.also { deltakelserUtenEnhet.add(deltakelse) }
            }
            // Grupper enhetsnavn og tell forekomster: "NAV Oslo" -> 82, "NAV Bergen" -> 67, osv.
            .groupingBy { it }
            .eachCount()

        return opprettResultat(deltakelser, deltakelserPerEnhet, veiledereMedFlereEnheter, ressurserMedTilknytninger, deltakelserUtenEnhet)
    }

    /**
     * Finner enhetsnavn for én deltakelse. Returnerer null hvis ingen enhet kan bestemmes.
     *
     * Steg:
     * 1. Slå opp veilederens NAV-ident i NOM-ressursene.
     * 2. Finn gyldige enheter på deltakelsens opprettelsesdato (inkl. fallback).
     * 3. Hvis flere gyldige enheter, velg den mest populære blant alle veiledere.
     */
    private fun finnEnhetForDeltakelse(
        deltakelse: DeltakelseInput,
        ressursLookup: Map<String, RessursMedAlleTilknytninger>,
        alleRessurser: List<RessursMedAlleTilknytninger>,
        veiledereMedFlereEnheter: MutableMap<String, List<OrgEnhetMedPeriode>>,
    ): String? {
        val navIdent = deltakelse.navIdent()
        val ressurs = ressursLookup[navIdent]

        // Veilederen finnes ikke i NOM — kan skyldes at de har sluttet eller ikke er registrert
        if (ressurs == null) {
            logger.warn("Fant ingen NOM-ressurs for NAV-ident {} (deltakelseId={})", navIdent, deltakelse.id)
            return null
        }

        // Finn enheter som var gyldige på tidspunktet deltakelsen ble opprettet
        val gyldigeEnheter = finnGyldigeEnheter(ressurs, deltakelse.opprettetDato)

        return when {
            // Ingen gyldige enheter funnet, heller ikke via fallback
            gyldigeEnheter.isEmpty() -> {
                logger.warn(
                    "Fant ingen gyldig enhet for NAV-ident {} på {} (deltakelseId={}, antallTilknytninger={})",
                    navIdent, deltakelse.opprettetDato, deltakelse.id, ressurs.orgTilknytninger.size
                )
                null
            }
            // Nøyaktig én gyldig enhet — bruk den direkte
            gyldigeEnheter.size == 1 -> gyldigeEnheter.first().navn
            // Flere gyldige enheter — velg den mest populære for å disambiguere
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
    }

    /**
     * Finner gyldige enheter for en veileder på en gitt dato.
     *
     * Bruker to strategier i prioritert rekkefølge:
     * 1. **Eksakt match**: Både tilknytning og orgEnhet må være gyldig på datoen.
     * 2. **Nærmeste tilknytning**: Velger tilknytningen med korteste absolutte avstand
     *    til datoen. Dekker gap i NOM ved enhetsbytte, sen NOM-registrering, og
     *    historiske deltakelser.
     */
    private fun finnGyldigeEnheter(
        ressurs: RessursMedAlleTilknytninger,
        opprettetDato: LocalDate,
    ): List<OrgEnhetMedPeriode> {
        // Strategi 1: Finn enheter der BÅDE tilknytningen og selve orgEnheten er gyldig på datoen.
        // Dobbeltsjekken er nødvendig fordi en tilknytning kan være aktiv selv om enheten er nedlagt.
        val eksaktGyldigeEnheter = ressurs.orgTilknytninger
            .filter { it.erGyldigPåTidspunkt(opprettetDato) }    // Er tilknytningen gyldig?
            .map { it.orgEnhet }
            .filter { it.erGyldigPåTidspunkt(opprettetDato) }    // Er selve enheten gyldig?
            .distinctBy { "${it.id}-${it.navn}" }

        if (eksaktGyldigeEnheter.isNotEmpty()) {
            return eksaktGyldigeEnheter
        }

        // Strategi 2: Nærmeste tilknytning — velg den med korteste absolutte avstand til datoen.
        // Dekker gap ved enhetsbytte, sen NOM-registrering, og historiske deltakelser.
        val tilknytningerMedAvstand = ressurs.orgTilknytninger
            .map { tilknytning ->
                val avstand = when {
                    opprettetDato.isBefore(tilknytning.gyldigFom) ->
                        ChronoUnit.DAYS.between(opprettetDato, tilknytning.gyldigFom)
                    tilknytning.gyldigTom != null && opprettetDato.isAfter(tilknytning.gyldigTom) ->
                        ChronoUnit.DAYS.between(tilknytning.gyldigTom, opprettetDato)
                    else -> 0L
                }
                tilknytning to avstand
            }
            .sortedBy { it.second }

        val nærmesteTilknytning = tilknytningerMedAvstand.firstOrNull()

        if (nærmesteTilknytning != null) {
            val kandidater = tilknytningerMedAvstand.joinToString { (tilknytning, avstand) ->
                "${tilknytning.orgEnhet.navn} (${tilknytning.gyldigFom}–${tilknytning.gyldigTom ?: "løpende"}, avstand: $avstand dager)"
            }
            logger.warn(
                "Nærmeste-tilknytning-fallback: NAV-ident {} på {} → {} (avstand: {} dager). Kandidater: [{}]",
                ressurs.navIdent, opprettetDato, nærmesteTilknytning.first.orgEnhet.navn,
                nærmesteTilknytning.second, kandidater
            )
            return listOf(nærmesteTilknytning.first.orgEnhet)
        }

        logger.warn(
            "Fant ingen enhet for NAV-ident {} på {} — ingen tilknytninger i NOM.",
            ressurs.navIdent, opprettetDato
        )
        return emptyList()
    }

    /**
     * Når en veileder har flere gyldige enheter på samme dato, velges den enheten
     * som flest veiledere totalt er tilknyttet. Dette er en heuristikk for å finne
     * den "riktige" enheten (typisk ungdomsteamet) fremfor sekundærtilknytninger
     * (f.eks. kontaktsenter, regionkontor).
     */
    private fun velgMestPopulæreEnhet(
        gyldigeEnheter: List<OrgEnhetMedPeriode>,
        alleRessurser: List<RessursMedAlleTilknytninger>,
        navIdent: String,
        opprettetDato: LocalDate,
        veiledereMedFlereEnheter: MutableMap<String, List<OrgEnhetMedPeriode>>,
    ): OrgEnhetMedPeriode {
        // Tell hvor mange tilknytninger hver enhet har totalt på tvers av alle veiledere
        val enhetPopularitet = beregnEnhetPopularitet(alleRessurser)
        // Velg enheten med flest tilknytninger totalt
        val mestPopulæreEnhet = gyldigeEnheter.maxByOrNull {
            enhetPopularitet["${it.id}-${it.navn}"] ?: 0
        } ?: gyldigeEnheter.first()

        loggFlereEnheterValg(gyldigeEnheter, enhetPopularitet, mestPopulæreEnhet, opprettetDato)
        veiledereMedFlereEnheter[navIdent] = gyldigeEnheter

        return mestPopulæreEnhet
    }

    /**
     * Beregner popularitet per enhet: antall ganger enheten forekommer som tilknytning
     * på tvers av alle veiledere. Brukes for å disambiguere når en veileder har flere enheter.
     */
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
            "NAV-ident hadde {} enheter på tidspunkt {} [{}], valgte den mest populære: {}-{}",
            gyldigeEnheter.size, opprettetDato, enhetInfo, valgtEnhet.id, valgtEnhet.navn
        )
    }

    /** Bygger resultatet med diagnostikk-data for feilsøking og overvåkning. */
    private fun opprettResultat(
        deltakelser: List<DeltakelseInput>,
        deltakelserPerEnhet: Map<String, Int>,
        veiledereMedFlereEnheter: Map<String, List<OrgEnhetMedPeriode>>,
        ressurserMedTilknytninger: List<RessursMedAlleTilknytninger>,
        deltakelserUtenEnhet: List<DeltakelseInput>,
    ): DeltakelsePerEnhetResultat {
        val antallUnikeNavIdenter = deltakelser
            .map { it.navIdent() }
            .toSet()
            .size

        logger.info("Fant {} unike NAV-identer fra {} deltakelser", antallUnikeNavIdenter, deltakelser.size)

        if (deltakelserUtenEnhet.isNotEmpty()) {
            // Logg hvilke veiledere som ikke kunne mappes — indikerer datakvalitetsproblemer i NOM
            val berørteNavIdenter = deltakelserUtenEnhet.map { it.navIdent() }.toSet()
            val berørteDeltakelseIder = deltakelserUtenEnhet.map { it.id }
            logger.warn(
                "{} deltakelser kunne ikke mappes til en enhet og er telt under '{}'. " +
                        "Berørte NAV-identer: {}, deltakelseIder: {}",
                deltakelserUtenEnhet.size, ENHET_SIKKERHETSNETT, berørteNavIdenter, berørteDeltakelseIder
            )
        }

        return DeltakelsePerEnhetResultat(
            deltakelserPerEnhet = deltakelserPerEnhet,
            diagnostikk = mapOf(
                "enhetPopularitet" to beregnEnhetPopularitet(ressurserMedTilknytninger),
                "veiledereMedFlereEnheter" to veiledereMedFlereEnheter,
                "totalAntallDeltakelser" to deltakelser.size,
                "antallUnikeNavIdenter" to antallUnikeNavIdenter,
                "deltakelserUtenEnhet" to mapOf(
                    "antall" to deltakelserUtenEnhet.size,
                    "berørteNavIdenter" to deltakelserUtenEnhet.map { it.navIdent() }.toSet()
                )
            )
        )
    }
}
