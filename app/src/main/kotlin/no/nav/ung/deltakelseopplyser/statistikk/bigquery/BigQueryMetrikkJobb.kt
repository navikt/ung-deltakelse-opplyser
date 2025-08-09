package no.nav.ung.deltakelseopplyser.statistikk.bigquery

import no.nav.ung.deltakelseopplyser.statistikk.oppgave.OppgaveStatistikkService
import no.nav.ung.deltakelseopplyser.statistikk.oppgave.OppgaveSvartidTabell
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@Profile(value = ["prod-gcp", "dev-gcp"])
class BigQueryMetrikkJobb(
    val bigQueryClient: BigQueryClient,
    val oppgaveStatistikkService: OppgaveStatistikkService
) {

    companion object {
        const val BIG_QUERY_DATASET = "ung_sak_statistikk_dataset"
    }

    @Scheduled(cron = "0 0 12 * * *")
    fun publiserOppgaveSvartidStatistikk() {
        val oppgaverMedSvarEllerEldreEnn14Dager =
            oppgaveStatistikkService.oppgaverMedSvarEllerEldreEnn14Dager()
        bigQueryClient.publish(BIG_QUERY_DATASET, OppgaveSvartidTabell, oppgaverMedSvarEllerEldreEnn14Dager)
    }

}
