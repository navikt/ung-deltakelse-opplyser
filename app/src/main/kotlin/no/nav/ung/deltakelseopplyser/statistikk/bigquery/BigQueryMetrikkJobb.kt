package no.nav.ung.deltakelseopplyser.statistikk.bigquery

import no.nav.k9.felles.konfigurasjon.konfig.Tid.TIDENES_BEGYNNELSE
import no.nav.ung.deltakelseopplyser.integration.leader.LeaderElectorService
import no.nav.ung.deltakelseopplyser.statistikk.deltakelse.AntallDeltakelserPerEnhetTabell
import no.nav.ung.deltakelseopplyser.statistikk.deltakelse.DeltakelseStatistikkService
import no.nav.ung.deltakelseopplyser.statistikk.deltaker.AntallDeltakereIUngdomsprogrammetRecord
import no.nav.ung.deltakelseopplyser.statistikk.deltaker.AntallDeltakereTabell
import no.nav.ung.deltakelseopplyser.statistikk.deltaker.DeltakerStatistikkService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@Profile(value = ["prod-gcp", "dev-gcp"])
class BigQueryMetrikkJobb(
    val bigQueryClient: BigQueryClient,
    val deltakerStatistikkService: DeltakerStatistikkService,
    val deltakelseStatistikkService: DeltakelseStatistikkService,
    val leaderElectorService: LeaderElectorService,
) {

    companion object {
        private val log = LoggerFactory.getLogger(BigQueryMetrikkJobb::class.java)
        const val BIG_QUERY_DATASET = "ung_deltakelse_opplyser_statistikk_dataset"

        private const val CRON_JOBB_HVER_TIME = "0 0 * * * *" // Hver time
    }

    /**
     * Publiserer statistikk for deltakere.
     */
    @Scheduled(cron = CRON_JOBB_HVER_TIME)
    fun publiserDeltakerStatistikk() {
        if (!leaderElectorService.erLeader()) {
            log.info("Denne instansen er ikke leder. Hopper over publisering av deltaker statistikk.")
            return
        }
        val antallDeltakereIUngdomsprogrammetRecord: AntallDeltakereIUngdomsprogrammetRecord =
            deltakerStatistikkService.antallDeltakereIUngdomsprogrammet()
        bigQueryClient.publish(
            BIG_QUERY_DATASET,
            AntallDeltakereTabell,
            listOf(antallDeltakereIUngdomsprogrammetRecord)
        ).also {
            loggPublisering(
                AntallDeltakereTabell.tabellNavn,
                1 // Vi publiserer kun én rad for antall deltakere
            )
        }
    }

    /**
     * Publiserer statistikk for deltakelser.
     */
    @Scheduled(cron = CRON_JOBB_HVER_TIME)
    fun publiserDeltakelseStatistikk() {
        if (!leaderElectorService.erLeader()) {
            log.info("Denne instansen er ikke leder. Hopper over publisering av deltakelse statistikk.")
            return
        }
        val antallDeltakelserPerEnhetStatistikk = deltakelseStatistikkService.antallDeltakelserPerEnhetStatistikk()
        bigQueryClient.publish(BIG_QUERY_DATASET, AntallDeltakelserPerEnhetTabell, antallDeltakelserPerEnhetStatistikk)
            .also {
                loggPublisering(AntallDeltakelserPerEnhetTabell.tabellNavn, antallDeltakelserPerEnhetStatistikk.size)
            }
    }

    private fun loggPublisering(
        tabellNavn: String,
        antallRader: Int,
    ) {
        log.info("Publiserte $tabellNavn: $antallRader (rader) til BigQuery.")
    }
}
