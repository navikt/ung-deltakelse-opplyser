package no.nav.ung.deltakelseopplyser.statistikk.deltakelse

import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseDAO
import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseRepository
import no.nav.ung.deltakelseopplyser.historikk.AuditorAwareImpl.Companion.VEILEDER_SUFFIX
import no.nav.ung.deltakelseopplyser.integration.nom.api.NomApiService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class DeltakelseStatistikkService(
    private val deltakelseRepository: DeltakelseRepository,
    private val nomApiService: NomApiService,
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(DeltakelseStatistikkService::class.java)
    }

    fun antallDeltakelserPerKontorStatistikk(): List<AntallDeltakelsePerEnhetStatistikkRecord> {
        val kjøringstidspunkt = ZonedDateTime.now()

        val alleDeltakelser: List<DeltakelseDAO> = deltakelseRepository.findAll()
        logger.info("Henter enheter for {} deltakelser", alleDeltakelser.size)

        // Grupper deltakelser per NAV-ident og hent unike identer i en operasjon
        val deltakelserPerNavIdent = alleDeltakelser
            .groupBy { deltakelse -> deltakelse.opprettetAv.replace(VEILEDER_SUFFIX, "").trim() }

        val unikeNavIdenter = deltakelserPerNavIdent.keys

        logger.info(
            "Fant {} unike NAV-identer fra {} deltakelser",
            unikeNavIdenter.size,
            alleDeltakelser.size
        )

        val resursserMedEnheter = nomApiService.hentResursserMedEnheter(unikeNavIdenter)

        // Teller antall deltakelser per enhet ved å mappe og gruppere på enhetsnavn og
        val deltakelserPerEnhet = resursserMedEnheter
            .asSequence()
            .filter { it.enheter.isNotEmpty() } // Filtrer ut ressurser uten enheter tidlig
            .map { ressursMedEnheter ->
                val antallDeltakelserForNavIdent = deltakelserPerNavIdent[ressursMedEnheter.navIdent]?.size ?: 0 // Finn antall deltakelser for denne veilederen

                // Kun tell deltakelser for den første enheten til hver person for å unngå dobbeltelling
                if (antallDeltakelserForNavIdent > 0) {
                    val enhet = ressursMedEnheter.enheter.first()

                    // Logg warning for personer med flere enheter
                    val enhetsnavn = enhet.navn
                    if (ressursMedEnheter.enheter.size > 1) {
                        logger.warn("NAV-ident har ${ressursMedEnheter.enheter.size} enheter [${ressursMedEnheter.enheter.map { "${it.id} - ${it.navn}" }}], teller kun på enhet ${enhet.id} - $enhetsnavn")
                    }

                    enhetsnavn to antallDeltakelserForNavIdent
                } else {
                    null
                }
            }
            .filterNotNull()
            .groupBy({ it.first }, { it.second }) // Gruppér på enhetsnavn
            .mapValues { (_, antallListe) -> antallListe.sum() } // Summer antall deltakelser per enhet

        // Samle diagnostikk-data for intern bruk
        val veiledereMedFlereEnheter = resursserMedEnheter
            .filter { it.enheter.size > 1 }
            .associate { it.navIdent to it.enheter }

        val antallUnikeEnheter = resursserMedEnheter
            .flatMap { it.enheter }
            .distinctBy { it.id }
            .size

        // Opprett statistikkrecords basert på akkumulerte tellinger
        return deltakelserPerEnhet.map { (enhetsNavn, antallDeltakelser) ->
            AntallDeltakelsePerEnhetStatistikkRecord(
                kontor = enhetsNavn,
                antallDeltakelser = antallDeltakelser,
                opprettetTidspunkt = kjøringstidspunkt,
                diagnostikk = mapOf(
                    "totalAntallDeltakelser" to alleDeltakelser.size,
                    "antallUnikeNavIdenter" to unikeNavIdenter.size,
                    "antallVeiledereMedFlereEnheter" to veiledereMedFlereEnheter,
                    "antallUnikeEnheter" to antallUnikeEnheter
                )
            )
        }.also {
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
