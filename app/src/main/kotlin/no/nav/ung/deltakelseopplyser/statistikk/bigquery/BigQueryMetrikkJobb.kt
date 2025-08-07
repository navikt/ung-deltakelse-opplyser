package no.nav.ung.deltakelseopplyser.statistikk.bigquery

import no.nav.ung.deltakelseopplyser.statistikk.oppgave.OppgaveStatistikkService
import no.nav.ung.deltakelseopplyser.statistikk.oppgave.OppgaveSvartidTabell
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@EnableScheduling
class BigQueryMetrikkJobb(
    val bigQueryClient: BigQueryClient,
    val oppgaveStatistikkService: OppgaveStatistikkService
) {

    @Scheduled(cron = "0 0 12 * * *")
    open fun publiserOppgaveSvartidStatistikk() {
        val oppgaverMedSvarEllerEldreEnn14Dager =
            oppgaveStatistikkService.oppgaverMedSvarEllerEldreEnn14Dager()
        bigQueryClient.publish(OppgaveSvartidTabell, oppgaverMedSvarEllerEldreEnn14Dager)
    }

}
