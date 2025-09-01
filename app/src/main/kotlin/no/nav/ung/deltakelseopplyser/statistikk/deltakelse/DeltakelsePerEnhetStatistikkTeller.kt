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
)

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

        // For diagnostikk: Map fra NAV-ident til liste av enheter for veiledere med flere enheter
        val veiledereMedFlereEnheter = mutableMapOf<String, List<NomApiService.OrgEnhetMedPeriode>>()

        val deltakelserPerEnhet = deltakelser
            .mapNotNull { deltakelse ->
                finnEnhetForDeltakelse(deltakelse, ressursLookup, ressurserMedTilknytninger, veiledereMedFlereEnheter)
            }
            .groupingBy { it }
            .eachCount()

        return opprettResultat(deltakelser, deltakelserPerEnhet, veiledereMedFlereEnheter, ressurserMedTilknytninger)
    }

    /**
     * Finn enhet for en deltakelse basert på opprettetAv (NAV-ident) og opprettetDato.
     * Hvis veileder har flere gyldige enheter på tidspunktet, velges den mest populære enheten.
     * Loggfører valg hvis veileder har flere enheter.
     * @param deltakelse Deltakelsen som skal analyseres.
     * @param ressursLookup Map fra NAV-ident til ressurs med alle tilknytninger.
     * @param alleRessurser Alle ressurser med tilknytninger for å beregne popularitet.
     * @param veiledereMedFlereEnheter Map som oppdateres med veiledere som har flere enheter.
     *
     * Returnerer enhetsnavn eller null hvis ingen enhet kan bestemmes.
     */
    private fun finnEnhetForDeltakelse(
        deltakelse: DeltakelseInput,
        ressursLookup: Map<String, NomApiService.RessursMedAlleTilknytninger>,
        alleRessurser: List<NomApiService.RessursMedAlleTilknytninger>,
        veiledereMedFlereEnheter: MutableMap<String, List<NomApiService.OrgEnhetMedPeriode>>
    ): String? {
        val navIdent = navIdent(deltakelse.opprettetAv)
        val ressurs = ressursLookup[navIdent]

        if (ressurs == null) {
            logger.warn("Fant ingen ressurs for NAV-ident $navIdent")
            return null
        }

        val gyldigeEnheter = finnGyldigeEnheter(ressurs, deltakelse.opprettetDato)

        if (gyldigeEnheter.isEmpty()) {
            logger.warn("Fant ingen gyldig enhet for NAV-ident $navIdent på tidspunkt ${deltakelse.opprettetDato}")
            return null
        }

        val valgtEnhet = velgEnhet(gyldigeEnheter, alleRessurser, navIdent, deltakelse.opprettetDato, veiledereMedFlereEnheter)
        return valgtEnhet.navn
    }

    /**
     * Ekstraherer NAV-ident fra opprettetAv ved å fjerne eventuelt veileder-suffiks.
     */
    private fun navIdent(opprettetAv: String): String {
        return opprettetAv.replace(VEILEDER_SUFFIX, "").trim()
    }

    /**
     * Finner alle gyldige enheter for en ressurs på et gitt tidspunkt.
     * Filtrerer både på organisasjonstilknytningens og enhetens gyldighetsperiode.
     * Returnerer en liste av unike enheter (basert på id og navn).
     * Hvis ingen enheter er gyldige, returneres en tom liste.
     * Hvis ressursen ikke har noen tilknytninger, returneres en tom liste.
     *
     * @param ressurs Ressursen med alle tilknytninger
     * @param opprettetDato Datoen som brukes for å sjekke gyldighet
     * @return Liste av gyldige enheter på det gitte tidspunktet eller tom liste hvis ingen er gyldige.
     */
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

    /**
     * Velger en enhet fra en liste av gyldige enheter.
     * Hvis det kun er én gyldig enhet, returneres denne.
     * Hvis det er flere gyldige enheter, velges den mest populære basert på forekomst i alle ressurser.
     * Loggfører valg hvis veileder har flere enheter.
     * Oppdaterer veiledereMedFlereEnheter med alle gyldige enheter for veilederen.
     *
     * @param gyldigeEnheter Liste av gyldige enheter for veilederen på tidspunktet.
     * @param alleRessurser Alle ressurser med tilknytninger for å beregne popularitet.
     * @param navIdent NAV-identen til veilederen.
     * @param opprettetDato Datoen som brukes for å sjekke gyldighet.
     * @param veiledereMedFlereEnheter Map som oppdateres med veiledere som har flere enheter.
     * @return Den valgte enheten.
     */
    private fun velgEnhet(
        gyldigeEnheter: List<NomApiService.OrgEnhetMedPeriode>,
        alleRessurser: List<NomApiService.RessursMedAlleTilknytninger>,
        navIdent: String,
        opprettetDato: LocalDate,
        veiledereMedFlereEnheter: MutableMap<String, List<NomApiService.OrgEnhetMedPeriode>>
    ): NomApiService.OrgEnhetMedPeriode {
        if (gyldigeEnheter.size == 1) {
            return gyldigeEnheter.first()
        }

        val enhetPopularitet = beregnEnhetPopularitet(alleRessurser)
        val mestPopulæreEnhet = gyldigeEnheter
            .maxByOrNull { enhet -> enhetPopularitet["${enhet.id}-${enhet.navn}"] ?: 0 }
            ?: gyldigeEnheter.first()

        loggFlereEnheterValg(gyldigeEnheter, enhetPopularitet, mestPopulæreEnhet, opprettetDato)
        veiledereMedFlereEnheter[navIdent] = gyldigeEnheter

        return mestPopulæreEnhet
    }

    /**
     * Beregner popularitet for hver enhet basert på forekomst i alle ressurser.
     * Returnerer en map fra "enhetId-enhetNavn" til antall forekomster.
     * Hvis ingen ressurser har tilknytninger, returneres en tom map.
     * @param ressurserMedTilknytninger Liste av ressurser med alle tilknytninger.
     * @return Map fra "enhetId-enhetNavn" til antall forekomster.
     */
    private fun beregnEnhetPopularitet(ressurserMedTilknytninger: List<NomApiService.RessursMedAlleTilknytninger>): Map<String, Int> {
        return ressurserMedTilknytninger
            .flatMap { it.orgTilknytninger }
            .map { it.orgEnhet }
            .groupingBy { "${it.id}-${it.navn}" }
            .eachCount()
    }

    /**
     * Logger valg av enhet når veileder har flere gyldige enheter.
     * Inkluderer informasjon om alle gyldige enheter og deres popularitet.
     * @param gyldigeEnheter Liste av gyldige enheter for veilederen.
     * @param enhetPopularitet Map fra "enhetId-enhetNavn" til antall forekomster.
     * @param valgtEnhet Den enheten som ble valgt.
     * @param opprettetDato Datoen som brukes for å sjekke gyldighet.
     */
    private fun loggFlereEnheterValg(
        gyldigeEnheter: List<NomApiService.OrgEnhetMedPeriode>,
        enhetPopularitet: Map<String, Int>,
        valgtEnhet: NomApiService.OrgEnhetMedPeriode,
        opprettetDato: LocalDate
    ) {
        val enhetInfo = gyldigeEnheter.map { enhet ->
            val popularitet = enhetPopularitet["${enhet.id}-${enhet.navn}"] ?: 0
            "${enhet.id}-${enhet.navn} (popularitet: $popularitet)"
        }

        logger.warn(
            "NAV-ident hadde ${gyldigeEnheter.size} enheter på tidspunkt $opprettetDato [$enhetInfo], valgte den mest populære: ${valgtEnhet.id}-${valgtEnhet.navn}"
        )
    }

    /**
     * Oppretter resultatobjektet med deltakelser per enhet og diagnostikk.
     * Inkluderer antall unike NAV-identer og enhet popularitet i diagnostikk.
     * Logger antall unike NAV-identer og totalt antall deltakelser.
     * @param deltakelser Liste av deltakelser som ble analysert.
     * @param deltakelserPerEnhet Map fra enhetsnavn til antall deltakelser.
     * @param veiledereMedFlereEnheter Map fra NAV-ident til liste av enheter for veiledere med flere enheter.
     * @param ressurserMedTilknytninger Liste av ressurser med alle tilknytninger brukt for å beregne popularitet.
     * @return Resultatobjekt med deltakelser per enhet og diagnostikk.
     */
    private fun opprettResultat(
        deltakelser: List<DeltakelseInput>,
        deltakelserPerEnhet: Map<String, Int>,
        veiledereMedFlereEnheter: Map<String, List<NomApiService.OrgEnhetMedPeriode>>,
        ressurserMedTilknytninger: List<NomApiService.RessursMedAlleTilknytninger>
    ): DeltakelsePerEnhetResultat {
        val antallUnikeNavIdenter = deltakelser
            .map { navIdent(it.opprettetAv) }
            .toSet()
            .size

        val enhetPopularitet = beregnEnhetPopularitet(ressurserMedTilknytninger)

        logger.info(
            "Fant {} unike NAV-identer fra {} deltakelser",
            antallUnikeNavIdenter,
            deltakelser.size
        )

        return DeltakelsePerEnhetResultat(
            deltakelserPerEnhet = deltakelserPerEnhet,
            diagnostikk = mapOf(
                "enhetPopularitet" to enhetPopularitet,
                "veiledereMedFlereEnheter" to veiledereMedFlereEnheter,
                "totalAntallDeltakelser" to deltakelser.size,
                "antallUnikeNavIdenter" to antallUnikeNavIdenter
            )
        )
    }
}
