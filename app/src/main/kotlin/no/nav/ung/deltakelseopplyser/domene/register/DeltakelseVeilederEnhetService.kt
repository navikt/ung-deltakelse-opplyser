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
     */
    fun prøvLagreEnhetForDeltakelse(deltakelseId: UUID, navIdent: String) {
        try {
            val ressurser = nomApiService.hentResursserMedAlleTilknytninger(setOf(navIdent))
            val ressurs = ressurser.firstOrNull()

            if (ressurs == null) {
                logger.warn("Kunne ikke lagre enhet-kobling for deltakelse $deltakelseId: Fant ingen NOM-ressurs for NAV-ident $navIdent")
                return
            }

            val enhet = resolveGyldigEnhet(ressurs, LocalDate.now())
            if (enhet == null) {
                logger.warn("Kunne ikke lagre enhet-kobling for deltakelse $deltakelseId: Fant ingen gyldig enhet for NAV-ident $navIdent")
                return
            }

            deltakelseVeilederEnhetRepository.save(
                DeltakelseVeilederEnhetDAO(
                    deltakelseId = deltakelseId,
                    navIdent = navIdent,
                    enhetId = enhet.id,
                    enhetNavn = enhet.navn,
                )
            )
            logger.info("Lagret enhet-kobling for deltakelse $deltakelseId: NAV-ident=$navIdent, enhet=${enhet.navn} (${enhet.id})")
        } catch (e: Exception) {
            logger.error("Feil ved lagring av enhet-kobling for deltakelse $deltakelseId, NAV-ident $navIdent. Fortsetter uten kobling.", e)
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
     * Returnerer resultat med antall vellykkede/feilede koblinger.
     */
    fun backfillEnhetKoblinger(
        deltakelser: List<BackfillInput>,
        ressurserMedTilknytninger: List<RessursMedAlleTilknytninger>,
    ): BackfillResultat {
        val eksisterendeKoblinger = deltakelseVeilederEnhetRepository
            .findAllByDeltakelseIdIn(deltakelser.map { it.deltakelseId })
            .map { it.deltakelseId }
            .toSet()

        val ressursLookup = ressurserMedTilknytninger.associateBy { it.navIdent }
        var antallOpprettet = 0
        var antallHoppetOver = 0
        val feiledeIdenter = mutableSetOf<String>()

        deltakelser.forEach { input ->
            if (input.deltakelseId in eksisterendeKoblinger) {
                antallHoppetOver++
                return@forEach
            }

            val ressurs = ressursLookup[input.navIdent]
            if (ressurs == null) {
                feiledeIdenter.add(input.navIdent)
                return@forEach
            }

            // Bruk nærmeste tilknytning uten toleransegrense for historiske deltakelser
            val enhet = resolveNærmesteEnhet(ressurs, input.opprettetDato)
            if (enhet == null) {
                feiledeIdenter.add(input.navIdent)
                return@forEach
            }

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

        logger.info("Backfill ferdig: $antallOpprettet opprettet, $antallHoppetOver hoppet over (eksisterte), ${feiledeIdenter.size} unike identer feilet: $feiledeIdenter")
        return BackfillResultat(antallOpprettet, antallHoppetOver, feiledeIdenter)
    }

    /**
     * Finner gyldig enhet for en veileder på en gitt dato.
     * Eksakt match, deretter bakover/fremover-fallback med 90 dager.
     */
    private fun resolveGyldigEnhet(ressurs: RessursMedAlleTilknytninger, dato: LocalDate): OrgEnhetMedPeriode? {
        val eksaktGyldig = ressurs.orgTilknytninger
            .filter { it.erGyldigPåTidspunkt(dato) }
            .map { it.orgEnhet }
            .filter { it.erGyldigPåTidspunkt(dato) }
            .firstOrNull()

        if (eksaktGyldig != null) return eksaktGyldig

        val toleranseDager = 90L
        return ressurs.orgTilknytninger
            .mapNotNull { tilknytning ->
                val avstand = beregnAvstand(tilknytning.gyldigFom, tilknytning.gyldigTom, dato)
                if (avstand <= toleranseDager) tilknytning.orgEnhet to avstand else null
            }
            .minByOrNull { it.second }
            ?.first
    }

    /**
     * Finner nærmeste tilknytning uten toleransegrense.
     * Brukes for backfill av historiske deltakelser.
     */
    private fun resolveNærmesteEnhet(ressurs: RessursMedAlleTilknytninger, dato: LocalDate): OrgEnhetMedPeriode? {
        if (ressurs.orgTilknytninger.isEmpty()) return null

        val eksaktGyldig = ressurs.orgTilknytninger
            .filter { it.erGyldigPåTidspunkt(dato) }
            .map { it.orgEnhet }
            .filter { it.erGyldigPåTidspunkt(dato) }
            .firstOrNull()

        if (eksaktGyldig != null) return eksaktGyldig

        return ressurs.orgTilknytninger
            .map { tilknytning -> tilknytning.orgEnhet to beregnAvstand(tilknytning.gyldigFom, tilknytning.gyldigTom, dato) }
            .minByOrNull { it.second }
            ?.first
    }

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
        val antallHoppetOver: Int,
        val feiledeNavIdenter: Set<String>,
    )
}

