package no.nav.ung.deltakelseopplyser.statistikk.deltakelse

import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseDAO
import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseRepository
import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseVeilederEnhetService
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
    private val deltakelseVeilederEnhetService: DeltakelseVeilederEnhetService,
    private val deltakelsePerEnhetStatistikkTeller: DeltakelsePerEnhetStatistikkTeller = DeltakelsePerEnhetStatistikkTeller()
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(DeltakelseStatistikkService::class.java)
    }

    fun antallDeltakelserPerEnhetStatistikk(kastFeilVedInkonsekventTelling: Boolean = true): List<AntallDeltakelsePerEnhetStatistikkRecord> {
        val kjøringstidspunkt = ZonedDateTime.now()

        val alleDeltakelser: List<DeltakelseDAO> = deltakelseRepository.findAll()
        logger.info("Henter enheter for {} deltakelser", alleDeltakelser.size)

        val deltakelseInputs = alleDeltakelser.map { deltakelse ->
            DeltakelseInput(
                id = deltakelse.id,
                opprettetAv = deltakelse.opprettetAv,
                opprettetDato = deltakelse.opprettetTidspunkt.atZone(ZoneOffset.UTC).toLocalDate()
            )
        }

        // Primærkilde: koblingstabellen (point-in-time snapshot av veileder→enhet)
        val enhetKoblinger = deltakelseVeilederEnhetService
            .hentEnhetNavnForDeltakelser(deltakelseInputs.map { it.id })

        val (medKobling, utenKobling) = deltakelseInputs.partition { it.id in enhetKoblinger }

        logger.info(
            "Koblingstabellen dekker {} av {} deltakelser. {} krever NOM-oppslag.",
            medKobling.size, deltakelseInputs.size, utenKobling.size
        )

        // Deltakelser med kobling → bruk enhetNavn direkte
        val deltakelserPerEnhetFraKobling: Map<String, Int> = medKobling
            .map { enhetKoblinger[it.id]!! }
            .groupingBy { it }
            .eachCount()

        // Deltakelser uten kobling → fallback til NOM-basert logikk
        var deltakelserPerEnhetFraNom: Map<String, Int> = emptyMap()
        var nomDiagnostikk: Map<Any, Any?> = emptyMap()

        if (utenKobling.isNotEmpty()) {
            val navIdenter = utenKobling
                .map { it.opprettetAv.replace(VEILEDER_SUFFIX, "").trim() }
                .toSet()

            logger.info("Henter NOM-data for {} unike NAV-identer (deltakelser uten kobling)", navIdenter.size)
            val ressurserMedAlleTilknytninger: List<RessursMedAlleTilknytninger> =
                nomApiService.hentResursserMedAlleTilknytninger(navIdenter)

            logger.info(
                "NOM returnerte {} ressurser for {} etterspurte identer",
                ressurserMedAlleTilknytninger.size, navIdenter.size
            )

            val nomResultat = deltakelsePerEnhetStatistikkTeller.tellAntallDeltakelserPerEnhet(
                deltakelser = utenKobling,
                ressurserMedTilknytninger = ressurserMedAlleTilknytninger
            )
            deltakelserPerEnhetFraNom = nomResultat.deltakelserPerEnhet
            nomDiagnostikk = nomResultat.diagnostikk
        }

        // Slå sammen resultatene fra kobling og NOM
        val samletDeltakelserPerEnhet = mutableMapOf<String, Int>()
        deltakelserPerEnhetFraKobling.forEach { (enhet, antall) ->
            samletDeltakelserPerEnhet.merge(enhet, antall, Int::plus)
        }
        deltakelserPerEnhetFraNom.forEach { (enhet, antall) ->
            samletDeltakelserPerEnhet.merge(enhet, antall, Int::plus)
        }

        // Logg endelig fordeling for feilsøking
        val fordeling = samletDeltakelserPerEnhet.entries
            .sortedByDescending { it.value }
            .joinToString { "${it.key}: ${it.value}" }
        logger.info("Statistikk-resultat fordeling: [{}]", fordeling)

        @Suppress("UNCHECKED_CAST")
        val diagnostikk = mapOf<Any, Any?>(
            "antallMedKobling" to medKobling.size,
            "antallUtenKobling" to utenKobling.size,
            "totalAntallDeltakelser" to deltakelseInputs.size,
            "nomDiagnostikk" to nomDiagnostikk,
        )

        val statistikkRecords = samletDeltakelserPerEnhet.map { (enhetsNavn, antallDeltakelser) ->
            AntallDeltakelsePerEnhetStatistikkRecord(
                kontor = enhetsNavn,
                antallDeltakelser = antallDeltakelser,
                opprettetTidspunkt = kjøringstidspunkt,
                diagnostikk = diagnostikk
            )
        }

        return statistikkRecords.also {
            verifiserKonekventTelling(it, alleDeltakelser, kastFeilVedInkonsekventTelling)
        }
    }

    private fun verifiserKonekventTelling(
        statistikkRecords: List<AntallDeltakelsePerEnhetStatistikkRecord>,
        alleDeltakelser: List<DeltakelseDAO>,
        kastFeilVedInkonsekventTelling: Boolean
    ) {
        val totalAntallDeltakelserStatistikk = statistikkRecords.sumOf { it.antallDeltakelser }
        if (kastFeilVedInkonsekventTelling && totalAntallDeltakelserStatistikk != alleDeltakelser.size) {
            throw IllegalStateException(
                "Inkonsekvent telling: Total antall deltakelser i statistikk ($totalAntallDeltakelserStatistikk) " +
                        "stemmer ikke overens med totalt antall deltakelser (${alleDeltakelser.size})"
            )
        } else {
            logger.info(
                "Verifisert konsekvent telling: {} deltakelser i statistikk = {} totalt",
                totalAntallDeltakelserStatistikk, alleDeltakelser.size
            )
        }
    }
}
