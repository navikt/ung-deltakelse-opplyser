package no.nav.ung.deltakelseopplyser.statistikk.deltakelse

import no.nav.nom.generated.hentressurser.OrgEnhet
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
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(DeltakelseStatistikkService::class.java)
    }

    fun antallDeltakelserPerKontorStatistikkV2(): List<AntallDeltakelsePerEnhetStatistikkRecord> {
        val alleDeltakelser: List<DeltakelseDAO> = deltakelseRepository.findAll()
        logger.info("Henter enheter for {} deltakelser", alleDeltakelser.size)

        /* Map alle deltakelser til unike (navIdent, opprettetDato) kombinasjoner. Dette for å håndtere at en veileder kan ha byttet enhet.
        Vi ønsker å telle hver deltakelse kun én gang, på den enheten veilederen hadde på tidspunktet deltakelsen ble opprettet.
        Det betyr at hvis en veileder har opprettet flere deltakelser over tid og potensielt byttet enhet, så må vi hente enhetsinfo for veilederen den datoen deltakelsen ble opprettet.
         */
        val navIdenterMedDeltakelseOpprettetTidspunkt = alleDeltakelser
            .map { deltakelse ->
                val navIdent = navIdent(deltakelse)
                val opprettetDato = deltakelse.opprettetTidspunkt.atZone(ZoneOffset.UTC).toLocalDate()
                NomApiService.NavIdentOgTidspunkt(navIdent, opprettetDato)
            }
            .toSet()

        logger.info("Fant {} unike (navIdent, dato) kombinasjoner", navIdenterMedDeltakelseOpprettetTidspunkt.size)

        val ressurserMedEnheter =
            nomApiService.hentResursserMedEnheterForTidspunkter(navIdenterMedDeltakelseOpprettetTidspunkt)

        // Beregn vektlegging av enheter basert på hvor ofte de forekommer blant alle veiledere
        val enhetPopularitet = ressurserMedEnheter
            .flatMap { it.enheter }
            .groupingBy { "${it.id}-${it.navn}" }
            .eachCount()

        logger.info("Beregnet popularitet for {} enheter", enhetPopularitet.size)

        // For diagnostikk - hent antall veiledere med flere enheter for det tidspunktet deltakelsen ble opprettet
        val antallVeiledereMedFlereEnheter = mutableMapOf<String, List<OrgEnhet>>()

        // Map hver deltakelse til enhetsnavn og tell
        val deltakelserPerEnhet: Map<String, Int> = alleDeltakelser
            .mapNotNull { deltakelse ->
                val navIdent = navIdent(deltakelse)
                val opprettetDato = deltakelse.opprettetTidspunkt.atZone(ZoneOffset.UTC).toLocalDate()
                val ressursMedEnheter = ressurserMedEnheter.find { it.navIdent == navIdent }

                if (ressursMedEnheter?.enheter?.isNotEmpty() == true) {
                    val enheter = ressursMedEnheter.enheter

                    val valgtEnhet = if (enheter.size == 1) {
                        enheter.first()
                    } else {
                        // Velg enheten med høyest popularitet (flest forekomster)
                        val populæresteEnhet = enheter
                            .maxByOrNull { enhet ->
                                enhetPopularitet["${enhet.id}-${enhet.navn}"] ?: 0
                            }
                            ?: enheter.first()

                        logger.warn(
                            "NAV-ident hadde ${enheter.size} enheter på tidspunkt $opprettetDato [${enheter.map { "${it.id}-${it.navn} (popularitet: ${enhetPopularitet["${it.id}-${it.navn}"] ?: 0})" }}], valgte den mest populære: ${populæresteEnhet.id}-${populæresteEnhet.navn}"
                        )

                        // Legg til i diagnostikk-data
                        antallVeiledereMedFlereEnheter[navIdent] = enheter

                        populæresteEnhet
                    }

                    valgtEnhet.navn
                } else {
                    logger.warn("Fant ingen gyldig enhet for NAV-ident $navIdent på tidspunkt $opprettetDato")
                    null
                }
            }
            .groupingBy { it }
            .eachCount()

        // Samle diagnostikk-data
        val unikeNavIdenter = alleDeltakelser
            .map { navIdent(it) }
            .toSet()

        logger.info(
            "Fant {} unike NAV-identer fra {} deltakelser",
            unikeNavIdenter.size,
            alleDeltakelser.size
        )

        // Hent antall unike enheter for diagnostikk
        val antallUnikeEnheter = nomApiService.hentResursserMedEnheter(unikeNavIdenter)
            .flatMap { it.enheter }
            .distinctBy { it.id }
            .size

        val kjøringstidspunkt = ZonedDateTime.now()

        // Opprett statistikkrecords basert på akkumulerte tellinger
        return deltakelserPerEnhet.map { (enhetsNavn, antallDeltakelser) ->
            AntallDeltakelsePerEnhetStatistikkRecord(
                kontor = enhetsNavn,
                antallDeltakelser = antallDeltakelser,
                opprettetTidspunkt = kjøringstidspunkt,
                diagnostikk = mapOf(
                    "totalAntallDeltakelser" to alleDeltakelser.size,
                    "antallUnikeNavIdenter" to unikeNavIdenter.size,
                    "antallVeiledereMedFlereEnheter" to antallVeiledereMedFlereEnheter,
                    "antallUnikeEnheter" to antallUnikeEnheter,
                    "enhetPopularitet" to enhetPopularitet
                )
            )
        }.also {
            verifiserKonekventTelling(it, alleDeltakelser)
        }
    }

    private fun navIdent(deltakelse: DeltakelseDAO): String = deltakelse.opprettetAv.replace(VEILEDER_SUFFIX, "").trim()

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
