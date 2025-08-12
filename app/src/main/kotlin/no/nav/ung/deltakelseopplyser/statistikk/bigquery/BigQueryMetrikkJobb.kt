package no.nav.ung.deltakelseopplyser.statistikk.bigquery

import no.nav.ung.deltakelseopplyser.statistikk.deltaker.AntallDeltakereAntallOppgaverFordelingRecord
import no.nav.ung.deltakelseopplyser.statistikk.deltaker.AntallDeltakereAntallOppgaverFordelingTabell
import no.nav.ung.deltakelseopplyser.statistikk.deltaker.AntallDeltakerePerOppgavetypeTabell
import no.nav.ung.deltakelseopplyser.statistikk.deltaker.AntallDeltakereRecord
import no.nav.ung.deltakelseopplyser.statistikk.deltaker.AntallDeltakereTabell
import no.nav.ung.deltakelseopplyser.statistikk.deltaker.DeltakerStatistikkService
import no.nav.ung.deltakelseopplyser.statistikk.oppgave.OppgaveStatistikkService
import no.nav.ung.deltakelseopplyser.statistikk.oppgave.OppgaveSvartidTabell
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@Profile(value = ["prod-gcp", "dev-gcp"])
class BigQueryMetrikkJobb(
    val bigQueryClient: BigQueryClient,
    val oppgaveStatistikkService: OppgaveStatistikkService,
    val deltakerStatistikkService: DeltakerStatistikkService,
) {

    companion object {
        private val log = LoggerFactory.getLogger(BigQueryMetrikkJobb::class.java)
        const val BIG_QUERY_DATASET = "ung_deltakelse_opplyser_statistikk_dataset"
        // Kjør hver 5. minutt
        private const val CRON_JOBB_HVER_5_MINUTTER = "0 0/5 * * * *" // Hver 5. minutt
        private const val CRON_JOBB_DAGLIG = "0 0 12 * * *" // Hver dag kl. 12:00
    }

    /**
     * Publiserer statistikk for oppgaver som har fått svar eller er eldre enn 14 dager.
     */
    @Scheduled(cron = CRON_JOBB_DAGLIG)
    fun publiserOppgaveSvartidStatistikk() {
        val oppgaverMedSvarEllerEldreEnn14Dager = oppgaveStatistikkService.oppgaverMedSvarEllerEldreEnn14Dager()
        bigQueryClient.publish(BIG_QUERY_DATASET, OppgaveSvartidTabell, oppgaverMedSvarEllerEldreEnn14Dager).also {
            loggPublisering(
                OppgaveSvartidTabell.tabellNavn,
                oppgaverMedSvarEllerEldreEnn14Dager.size
            )
        }
    }

    /**
     * Publiserer statistikk for deltakere.
     */
    @Scheduled(cron = CRON_JOBB_DAGLIG)
    fun publiserDeltakerStatistikk() {
        val antallDeltakereRecord: AntallDeltakereRecord = deltakerStatistikkService.antallDeltakere()
        bigQueryClient.publish(BIG_QUERY_DATASET, AntallDeltakereTabell, listOf(antallDeltakereRecord)).also {
            loggPublisering(
                AntallDeltakereTabell.tabellNavn,
                1 // Vi publiserer kun én rad for antall deltakere
            )
        }

        val antallOppgaverAntallDeltakereFordelingRecord: List<AntallDeltakereAntallOppgaverFordelingRecord> =
            deltakerStatistikkService.antallDeltakereEtterAntallOppgaverFordeling()

        bigQueryClient.publish(
            BIG_QUERY_DATASET,
            AntallDeltakereAntallOppgaverFordelingTabell,
            antallOppgaverAntallDeltakereFordelingRecord
        ).also {
            loggPublisering(
                AntallDeltakereAntallOppgaverFordelingTabell.tabellNavn,
                antallOppgaverAntallDeltakereFordelingRecord.size
            )
        }

        val antallDeltakerePerOppgavetype = deltakerStatistikkService.antallDeltakerePerOppgavetype()
        bigQueryClient.publish(
            BIG_QUERY_DATASET,
            AntallDeltakerePerOppgavetypeTabell,
            antallDeltakerePerOppgavetype
        ).also {
            loggPublisering(
                AntallDeltakereTabell.tabellNavn,
                antallDeltakerePerOppgavetype.size
            )
        }
    }

    private fun loggPublisering(
        tabellNavn: String,
        antallRader: Int,
    ) {
        log.info("Publiserte $tabellNavn: $antallRader (rader) til BigQuery.")
    }
}
