package no.nav.ung.deltakelseopplyser.domene.register

import no.nav.ung.deltakelseopplyser.integration.nom.api.NomApiService
import no.nav.ung.deltakelseopplyser.integration.nom.api.domene.OrgEnhetMedPeriode
import no.nav.ung.deltakelseopplyser.integration.nom.api.domene.RessursMedAlleTilknytninger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class DeltakelseVeilederEnhetService(
    private val deltakelseVeilederEnhetRepository: DeltakelseVeilederEnhetRepository,
    private val nomApiService: NomApiService,
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(DeltakelseVeilederEnhetService::class.java)
    }

    /**
     * Henter alle koblinger for gitte deltakelse-IDer.
     * Returnerer en map fra deltakelseId til enhetNavn.
     */
    fun hentEnhetNavnForDeltakelser(deltakelseIder: List<UUID>): Map<UUID, String> {
        if (deltakelseIder.isEmpty()) return emptyMap()
        return deltakelseVeilederEnhetRepository.findAllByDeltakelseIdIn(deltakelseIder)
            .associate { it.deltakelseId to it.enhetNavn }
    }

    /**
     * Henter enhet-kobling for en enkelt deltakelse. Returnerer null hvis ikke funnet.
     */
    fun hentEnhetKoblingForDeltakelse(deltakelseId: UUID): DeltakelseVeilederEnhetDAO? {
        return deltakelseVeilederEnhetRepository.findByDeltakelseId(deltakelseId)
    }

    /**
     * Prøver å resolve veilederens nåværende enhet fra NOM og lagre koblingen.
     * Feiler stille (try-catch) slik at innmeldingen ikke blokkeres.
     *
     * Når veilederen har flere gyldige enheter (f.eks. ungdomsteam + kontaktsenter),
     * velges den mest populære enheten blant alle veiledere i NOM-datasettet.
     */
    fun prøvLagreEnhetForDeltakelse(deltakelseId: UUID, navIdent: String) {
        try {
            val ressurser = nomApiService.hentResursserMedAlleTilknytninger(setOf(navIdent))
            val ressurs = ressurser.firstOrNull()

            if (ressurs == null) {
                logger.warn(
                    "Kunne ikke lagre enhet-kobling for deltakelse {}: Fant ingen NOM-ressurs for NAV-ident {} (NOM returnerte {} ressurser)",
                    deltakelseId, navIdent, ressurser.size
                )
                return
            }

            val dato = LocalDate.now()
            val kandidater = resolveGyldigeEnheter(ressurs, dato)
            if (kandidater.isEmpty()) {
                val tilknytninger = ressurs.orgTilknytninger.joinToString { t ->
                    "${t.orgEnhet.navn} (${t.gyldigFom}–${t.gyldigTom ?: "løpende"})"
                }
                logger.warn(
                    "Kunne ikke lagre enhet-kobling for deltakelse {}: Fant ingen gyldig enhet for NAV-ident {} på {} " +
                            "(antallTilknytninger={}, tilknytninger=[{}])",
                    deltakelseId, navIdent, dato, ressurs.orgTilknytninger.size, tilknytninger
                )
                return
            }

            // Disambiguer ved flere gyldige enheter — bruk popularitet
            val enhet = if (kandidater.size > 1) {
                val enhetPopularitet = beregnEnhetPopularitet(ressurser)
                val valgt = velgMestPopulær(kandidater, enhetPopularitet)
                logger.info(
                    "Deltakelse {}: NAV-ident {} har {} gyldige enheter på {}, velger mest populære: {} ({}). Kandidater: [{}]",
                    deltakelseId, navIdent, kandidater.size, dato, valgt.navn, valgt.id,
                    kandidater.joinToString { "${it.navn} (${it.id})" }
                )
                valgt
            } else {
                kandidater.first()
            }

            deltakelseVeilederEnhetRepository.save(
                DeltakelseVeilederEnhetDAO(
                    deltakelseId = deltakelseId,
                    navIdent = navIdent,
                    enhetId = enhet.id,
                    enhetNavn = enhet.navn,
                )
            )
            logger.info(
                "Lagret enhet-kobling for deltakelse {}: NAV-ident={}, enhet={} ({})",
                deltakelseId, navIdent, enhet.navn, enhet.id
            )
        } catch (e: Exception) {
            logger.error(
                "Feil ved lagring av enhet-kobling for deltakelse {}, NAV-ident {}. Fortsetter uten kobling.",
                deltakelseId, navIdent, e
            )
        }
    }

    /**
     * Oppdaterer (upsert) enhet-kobling for en spesifikk deltakelse.
     * Brukes via diagnostikk-endepunktet for manuell korrigering.
     */
    fun oppdaterEnhetKobling(deltakelseId: UUID, navIdent: String, enhetId: String, enhetNavn: String): DeltakelseVeilederEnhetDAO {
        val eksisterende = deltakelseVeilederEnhetRepository.findByDeltakelseId(deltakelseId)

        return if (eksisterende != null) {
            eksisterende.enhetId = enhetId
            eksisterende.enhetNavn = enhetNavn
            deltakelseVeilederEnhetRepository.save(eksisterende).also {
                logger.info("Oppdatert enhet-kobling for deltakelse $deltakelseId: enhet=$enhetNavn ($enhetId)")
            }
        } else {
            deltakelseVeilederEnhetRepository.save(
                DeltakelseVeilederEnhetDAO(
                    deltakelseId = deltakelseId,
                    navIdent = navIdent,
                    enhetId = enhetId,
                    enhetNavn = enhetNavn,
                )
            ).also {
                logger.info("Opprettet enhet-kobling for deltakelse $deltakelseId: NAV-ident=$navIdent, enhet=$enhetNavn ($enhetId)")
            }
        }
    }

    /**
     * Backfill: Lagrer enhets-koblinger for en liste med deltakelser basert på NOM-data.
     * Bruker popularitets-disambiguering for å velge riktig enhet når en veileder har
     * flere samtidige tilknytninger (f.eks. ungdomsteam + kontaktsenter).
     *
     * @param force Hvis true, overskriver eksisterende koblinger. Nyttig for re-backfill.
     */
    fun backfillEnhetKoblinger(
        deltakelser: List<BackfillInput>,
        ressurserMedTilknytninger: List<RessursMedAlleTilknytninger>,
        force: Boolean = false,
    ): BackfillResultat {
        val eksisterendeKoblinger = if (force) {
            emptySet()
        } else {
            deltakelseVeilederEnhetRepository
                .findAllByDeltakelseIdIn(deltakelser.map { it.deltakelseId })
                .map { it.deltakelseId }
                .toSet()
        }

        val ressursLookup = ressurserMedTilknytninger.associateBy { it.navIdent }
        val enhetPopularitet = beregnEnhetPopularitet(ressurserMedTilknytninger)

        var antallOpprettet = 0
        var antallOppdatert = 0
        var antallHoppetOver = 0
        val identerUtenRessurs = mutableSetOf<String>()
        val identerUtenGyldigEnhet = mutableSetOf<String>()

        deltakelser.forEach { input ->
            if (!force && input.deltakelseId in eksisterendeKoblinger) {
                antallHoppetOver++
                return@forEach
            }

            val ressurs = ressursLookup[input.navIdent]
            if (ressurs == null) {
                identerUtenRessurs.add(input.navIdent)
                return@forEach
            }

            val kandidater = resolveGyldigeEnheter(ressurs, input.opprettetDato)
            if (kandidater.isEmpty()) {
                identerUtenGyldigEnhet.add(input.navIdent)
                logger.debug(
                    "Backfill: Ingen gyldig enhet for NAV-ident {} på {} (deltakelseId={}, antallTilknytninger={})",
                    input.navIdent, input.opprettetDato, input.deltakelseId, ressurs.orgTilknytninger.size
                )
                return@forEach
            }

            val enhet = if (kandidater.size > 1) {
                val valgt = velgMestPopulær(kandidater, enhetPopularitet)
                logger.debug(
                    "Backfill: NAV-ident {} har {} kandidater på {}, velger {} ({})",
                    input.navIdent, kandidater.size, input.opprettetDato, valgt.navn, valgt.id
                )
                valgt
            } else {
                kandidater.first()
            }

            if (force) {
                oppdaterEnhetKobling(input.deltakelseId, input.navIdent, enhet.id, enhet.navn)
                antallOppdatert++
            } else {
                deltakelseVeilederEnhetRepository.save(
                    DeltakelseVeilederEnhetDAO(
                        deltakelseId = input.deltakelseId,
                        navIdent = input.navIdent,
                        enhetId = enhet.id,
                        enhetNavn = enhet.navn,
                    )
                )
                antallOpprettet++
            }
        }

        val feiledeIdenter = identerUtenRessurs + identerUtenGyldigEnhet
        logger.info(
            "Backfill ferdig (force={}): {} opprettet, {} oppdatert, {} hoppet over (eksisterte), {} feilet " +
                    "(ingen NOM-ressurs: {}, ingen gyldig enhet: {})",
            force, antallOpprettet, antallOppdatert, antallHoppetOver, feiledeIdenter.size,
            identerUtenRessurs, identerUtenGyldigEnhet
        )
        return BackfillResultat(antallOpprettet, antallOppdatert, antallHoppetOver, feiledeIdenter)
    }

    /**
     * Finner alle gyldige enheter for en veileder på en gitt dato.
     * Returnerer en liste slik at kalleren kan disambiguere (f.eks. via popularitet).
     *
     * Strategier:
     * 1. Eksakt match: Tilknytning og orgEnhet begge gyldige på datoen.
     * 2. Nærmeste tilknytning: Korteste absolutte avstand til datoen.
     */
    private fun resolveGyldigeEnheter(ressurs: RessursMedAlleTilknytninger, dato: LocalDate): List<OrgEnhetMedPeriode> {
        if (ressurs.orgTilknytninger.isEmpty()) return emptyList()

        val eksaktGyldige = ressurs.orgTilknytninger
            .filter { it.erGyldigPåTidspunkt(dato) }
            .map { it.orgEnhet }
            .filter { it.erGyldigPåTidspunkt(dato) }
            .distinctBy { "${it.id}-${it.navn}" }

        if (eksaktGyldige.isNotEmpty()) return eksaktGyldige

        val tilknytningerMedAvstand = ressurs.orgTilknytninger
            .map { tilknytning -> tilknytning.orgEnhet to beregnAvstand(tilknytning.gyldigFom, tilknytning.gyldigTom, dato) }
            .sortedBy { it.second }

        val minAvstand = tilknytningerMedAvstand.firstOrNull()?.second ?: return emptyList()

        // Returner alle med samme minimale avstand (kan være flere like nære)
        return tilknytningerMedAvstand
            .filter { it.second == minAvstand }
            .map { it.first }
            .distinctBy { "${it.id}-${it.navn}" }
    }

    /**
     * Beregner popularitet per enhet: antall ganger enheten forekommer som tilknytning
     * på tvers av alle veiledere. Brukes for å disambiguere når en veileder har flere enheter.
     */
    private fun beregnEnhetPopularitet(ressurser: List<RessursMedAlleTilknytninger>): Map<String, Int> =
        ressurser
            .flatMap { it.orgTilknytninger }
            .map { it.orgEnhet }
            .groupingBy { "${it.id}-${it.navn}" }
            .eachCount()

    /**
     * Velger den mest populære enheten fra en liste med kandidater.
     * Faller tilbake til første enhet hvis ingen popularitetsdata finnes.
     */
    private fun velgMestPopulær(kandidater: List<OrgEnhetMedPeriode>, popularitet: Map<String, Int>): OrgEnhetMedPeriode =
        kandidater.maxByOrNull { popularitet["${it.id}-${it.navn}"] ?: 0 } ?: kandidater.first()

    /**
     * Beregner den absolutte avstanden i dager mellom en tilknytningsperiode og en dato.
     * Returnerer 0 hvis datoen er innenfor perioden.
     */
    private fun beregnAvstand(gyldigFom: LocalDate, gyldigTom: LocalDate?, dato: LocalDate): Long {
        return when {
            dato.isBefore(gyldigFom) -> ChronoUnit.DAYS.between(dato, gyldigFom)
            gyldigTom != null && dato.isAfter(gyldigTom) -> ChronoUnit.DAYS.between(gyldigTom, dato)
            else -> 0L
        }
    }

    data class BackfillInput(
        val deltakelseId: UUID,
        val navIdent: String,
        val opprettetDato: LocalDate,
    )

    data class BackfillResultat(
        val antallOpprettet: Int,
        val antallOppdatert: Int,
        val antallHoppetOver: Int,
        val feiledeNavIdenter: Set<String>,
    )
}
