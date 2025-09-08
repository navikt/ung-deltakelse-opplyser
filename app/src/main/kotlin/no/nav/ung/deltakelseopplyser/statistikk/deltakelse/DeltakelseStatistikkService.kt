package no.nav.ung.deltakelseopplyser.statistikk.deltakelse

import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseDAO
import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseRepository
import no.nav.ung.deltakelseopplyser.historikk.AuditorAwareImpl.Companion.VEILEDER_SUFFIX
import no.nav.ung.deltakelseopplyser.integration.nom.api.NomApiService
import no.nav.ung.deltakelseopplyser.integration.nom.api.domene.RessursMedAlleTilknytninger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Service
class DeltakelseStatistikkService(
    private val deltakelseRepository: DeltakelseRepository,
    private val nomApiService: NomApiService,
    private val deltakelsePerEnhetStatistikkTeller: DeltakelsePerEnhetStatistikkTeller = DeltakelsePerEnhetStatistikkTeller()
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(DeltakelseStatistikkService::class.java)
    }

    fun antallDeltakelserPerEnhetStatistikk(): List<AntallDeltakelsePerEnhetStatistikkRecord> {
        val kjøringstidspunkt = ZonedDateTime.now()

        val alleDeltakelser: List<DeltakelseDAO> = deltakelseRepository.findAll()
        logger.info("Henter enheter for {} deltakelser", alleDeltakelser.size)

        // Konverter til input-format for beregner
        val deltakelseInputs = alleDeltakelser.map { deltakelse ->
            DeltakelseInput(
                id = deltakelse.id,
                opprettetAv = deltakelse.opprettetAv,
                opprettetDato = deltakelse.opprettetTidspunkt.atZone(ZoneOffset.UTC).toLocalDate()
            )
        }

        // Hent alle unike navIdenter fra deltakelsene
        val navIdenter = deltakelseInputs
            .map { it.opprettetAv.replace(VEILEDER_SUFFIX, "").trim() }
            .toSet()

        logger.info("Fant {} unike NAV-identer", navIdenter.size)

        // Hent alle tilknytninger fra NOM API (ufiltrert med periodeinformasjon)
        // Vi henter ufiltrerte data slik at periodefiltrering kan skje i beregner og testes
        val ressurserMedAlleTilknytninger: List<RessursMedAlleTilknytninger> = nomApiService.hentResursserMedAlleTilknytninger(navIdenter)

        val deltakelsePerEnhetResultat = deltakelsePerEnhetStatistikkTeller.tellAntallDeltakelserPerEnhet(
            deltakelser = deltakelseInputs,
            ressurserMedTilknytninger = ressurserMedAlleTilknytninger
        )

        // Konverter til statistikk-records
        val statistikkRecords = deltakelsePerEnhetResultat.deltakelserPerEnhet.map { (enhetsNavn, antallDeltakelser) ->
            AntallDeltakelsePerEnhetStatistikkRecord(
                kontor = enhetsNavn,
                antallDeltakelser = antallDeltakelser,
                opprettetTidspunkt = kjøringstidspunkt,
                diagnostikk = deltakelsePerEnhetResultat.diagnostikk
            )
        }

        return statistikkRecords.also {
            verifiserKonekventTelling(it, alleDeltakelser)
        }
    }

    private fun verifiserKonekventTelling(
        statistikkRecords: List<AntallDeltakelsePerEnhetStatistikkRecord>,
        alleDeltakelser: List<DeltakelseDAO>,
    ) {
        val totalAntallDeltakelserStatistikk = statistikkRecords.sumOf { it.antallDeltakelser }
        if (totalAntallDeltakelserStatistikk != alleDeltakelser.size) {
            throw IllegalStateException("Inkonsekvent telling: Total antall deltakelser i statistikk (${totalAntallDeltakelserStatistikk}) stemmer ikke overens med totalt antall deltakelser (${alleDeltakelser.size})")
        } else {
            logger.info("Verifisert at total antall deltakelser i statistikk (${totalAntallDeltakelserStatistikk}) stemmer overens med totalt antall deltakelser for enhetene (${alleDeltakelser.size})")
        }
    }
}
