package no.nav.ung.deltakelseopplyser.statistikk.deltakelse

import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseDAO
import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseRepository
import no.nav.ung.deltakelseopplyser.historikk.AuditorAwareImpl.Companion.VEILEDER_SUFFIX
import no.nav.ung.deltakelseopplyser.integration.nom.api.NomApiService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Service
class DeltakelseStatistikkService(
    private val deltakelseRepository: DeltakelseRepository,
    private val nomApiService: NomApiService,
    private val statistikkBeregner: DeltakelseStatistikkBeregner = DeltakelseStatistikkBeregner()
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(DeltakelseStatistikkService::class.java)
    }

    fun antallDeltakelserPerKontorStatistikkV2(): List<AntallDeltakelsePerEnhetStatistikkRecord> {
        val kjøringstidspunkt = ZonedDateTime.now()

        val alleDeltakelser: List<DeltakelseDAO> = deltakelseRepository.findAll()
        logger.info("Henter enheter for {} deltakelser", alleDeltakelser.size)

        // Konverter til input-format
        val deltakelseInputs = alleDeltakelser.map { deltakelse ->
            DeltakelseInput(
                id = deltakelse.id,
                opprettetAv = deltakelse.opprettetAv,
                opprettetDato = deltakelse.opprettetTidspunkt.atZone(ZoneOffset.UTC).toLocalDate()
            )
        }

        // Hent alle unike (navIdent, opprettetDato) kombinasjoner
        val navIdenterMedTidspunkt = deltakelseInputs
            .map { deltakelse ->
                val navIdent = deltakelse.opprettetAv.replace(VEILEDER_SUFFIX, "").trim()
                NomApiService.NavIdentOgTidspunkt(navIdent, deltakelse.opprettetDato)
            }
            .toSet()

        logger.info("Fant {} unike (navIdent, dato) kombinasjoner", navIdenterMedTidspunkt.size)

        // Hent enhetsinfo fra NOM API
        val ressurserMedEnheter = nomApiService.hentResursserMedEnheterForTidspunkter(navIdenterMedTidspunkt)

        // Konverter til input-format for beregner
        val ressurserMedEnheterInput = ressurserMedEnheter.map { ressurs ->
            RessursMedEnheterInput(
                navIdent = ressurs.navIdent,
                enheter = ressurs.enheter.map { enhet ->
                    OrgEnhetInput(
                        id = enhet.id,
                        navn = enhet.navn
                    )
                }
            )
        }

        // Utfør beregning med den isolerte beregneren
        val beregningResultat = statistikkBeregner.beregnAntallDeltakelserPerEnhet(
            deltakelser = deltakelseInputs,
            ressurserMedEnheter = ressurserMedEnheterInput
        )

        // Konverter til statistikk-records
        val statistikkRecords = statistikkBeregner.konverterTilStatistikkRecords(
            deltakelsePerEnhetResultat = beregningResultat,
            kjoringstidspunkt = kjøringstidspunkt
        )

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
