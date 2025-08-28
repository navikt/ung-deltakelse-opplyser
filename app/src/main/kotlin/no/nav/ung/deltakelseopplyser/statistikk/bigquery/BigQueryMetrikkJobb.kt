package no.nav.ung.deltakelseopplyser.statistikk.bigquery

import no.nav.ung.deltakelseopplyser.statistikk.deltaker.AntallDeltakereIUngdomsprogrammetRecord
import no.nav.ung.deltakelseopplyser.statistikk.deltaker.AntallDeltakerePerOppgavetypeTabell
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

        private const val CRON_JOBB_HVER_TIME = "0 0 * * * *" // Hver time
    }

    /**
     * Publiserer statistikk for oppgaver som har fått svar eller er eldre enn 14 dager.
     */
    @Scheduled(cron = CRON_JOBB_HVER_TIME)
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
    @Scheduled(cron = CRON_JOBB_HVER_TIME)
    fun publiserDeltakerStatistikk() {
        val antallDeltakereIUngdomsprogrammetRecord: AntallDeltakereIUngdomsprogrammetRecord = deltakerStatistikkService.antallDeltakereIUngdomsprogrammet()
        bigQueryClient.publish(BIG_QUERY_DATASET, AntallDeltakereTabell, listOf(antallDeltakereIUngdomsprogrammetRecord)).also {
            loggPublisering(
                AntallDeltakereTabell.tabellNavn,
                1 // Vi publiserer kun én rad for antall deltakere
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
