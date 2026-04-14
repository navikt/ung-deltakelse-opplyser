package no.nav.ung.deltakelseopplyser.statistikk.deltakelse

import no.nav.ung.deltakelseopplyser.historikk.AuditorAwareImpl.Companion.VEILEDER_SUFFIX
import no.nav.ung.deltakelseopplyser.integration.nom.api.domene.OrgEnhetMedPeriode
import no.nav.ung.deltakelseopplyser.integration.nom.api.domene.RessursMedAlleTilknytninger
import org.slf4j.LoggerFactory
import java.time.LocalDate
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
 * 3. Hvis ingen enhet er gyldig på eksakt dato, brukes en fallback med 90-dagers toleranse.
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
            logger.warn("Fant ingen ressurs for NAV-ident $navIdent")
            return null
        }

        // Finn enheter som var gyldige på tidspunktet deltakelsen ble opprettet
        val gyldigeEnheter = finnGyldigeEnheter(ressurs, deltakelse.opprettetDato)

        return when {
            // Ingen gyldige enheter funnet, heller ikke via fallback
            gyldigeEnheter.isEmpty() -> {
                logger.warn("Fant ingen gyldig enhet for NAV-ident $navIdent på tidspunkt ${deltakelse.opprettetDato}")
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
     * Bruker to strategier:
     * 1. **Eksakt match**: Både tilknytning og orgEnhet må være gyldig på datoen.
     * 2. **Fallback (90 dager)**: Hvis ingen eksakt match, brukes den sist utløpte tilknytningen
     *    som sluttet innen 90 dager før datoen. Dette dekker gap i NOM ved enhetsbytte.
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

        // Strategi 2 (fallback): Finn siste gyldige enhet innen toleranseperiode.
        // Toleranseperioden overbrygger korte gap i NOM-data som oppstår ved enhetsbytte,
        // der den gamle tilknytningen har utløpt men den nye ikke er registrert ennå.
        val toleranseDager = 90L

        val sisteGyldigeEnhet = ressurs.orgTilknytninger
            .filter { tilknytning ->
                // Tilknytningen må ha startet før (eller på) opprettelsesdatoen
                !tilknytning.gyldigFom.isAfter(opprettetDato) &&
                        // Tilknytningen må ha en sluttdato (dvs. den har utløpt)
                        tilknytning.gyldigTom != null &&
                        // Sluttdatoen må ikke ligge mer enn 90 dager før opprettelsesdatoen
                        !tilknytning.gyldigTom.isBefore(opprettetDato.minusDays(toleranseDager))
            }
            // Velg den som utløp sist (mest sannsynlig riktig enhet)
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
            "NAV-ident hadde ${gyldigeEnheter.size} enheter på tidspunkt $opprettetDato [$enhetInfo], " +
                    "valgte den mest populære: ${valgtEnhet.id}-${valgtEnhet.navn}"
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

        logger.info("Fant $antallUnikeNavIdenter unike NAV-identer fra ${deltakelser.size} deltakelser")

        if (deltakelserUtenEnhet.isNotEmpty()) {
            // Logg hvilke veiledere som ikke kunne mappes — indikerer datakvalitetsproblemer i NOM
            val berørteNavIdenter = deltakelserUtenEnhet.map { it.navIdent() }.toSet()
            logger.warn(
                "${deltakelserUtenEnhet.size} deltakelser kunne ikke mappes til en enhet og er telt under '$ENHET_SIKKERHETSNETT'. " +
                        "Berørte NAV-identer: $berørteNavIdenter"
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
