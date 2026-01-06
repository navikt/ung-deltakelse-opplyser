package no.nav.ung.deltakelseopplyser.statistikk.bigquery

import no.nav.ung.deltakelseopplyser.integration.leader.LeaderElectorService
import no.nav.ung.deltakelseopplyser.statistikk.deltakelse.AntallDeltakelserPerEnhetTabell
import no.nav.ung.deltakelseopplyser.statistikk.deltakelse.DeltakelseStatistikkService
import no.nav.ung.deltakelseopplyser.statistikk.deltaker.AntallDeltakereIUngdomsprogrammetRecord
import no.nav.ung.deltakelseopplyser.statistikk.deltaker.AntallDeltakerePerOppgavetypeTabell
import no.nav.ung.deltakelseopplyser.statistikk.deltaker.AntallDeltakereTabell
import no.nav.ung.deltakelseopplyser.statistikk.deltaker.DeltakerStatistikkService
import no.nav.ung.deltakelseopplyser.statistikk.oppgave.BekreftAvvikOppgaveTabell
import no.nav.ung.deltakelseopplyser.statistikk.oppgave.OppgaveStatistikkService
import no.nav.ung.deltakelseopplyser.statistikk.oppgave.OppgaveSvartidTabell
import no.nav.ung.deltakelseopplyser.statistikk.oppgave.RapporterInntektOppgaveTabell
import no.nav.ung.kodeverk.uttak.Tid
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
@Profile(value = ["prod-gcp", "dev-gcp"])
class BigQueryMetrikkJobb(
    val bigQueryClient: BigQueryClient,
    val oppgaveStatistikkService: OppgaveStatistikkService,
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
     * Publiserer statistikk for oppgaver som har fått svar eller er eldre enn 14 dager.
     */
    @Scheduled(cron = CRON_JOBB_HVER_TIME)
    fun publiserOppgaveSvartidStatistikk() {
        if (!leaderElectorService.erLeader()) {
            log.info("Denne instansen er ikke leder. Hopper over publisering av oppgave svartid statistikk.")
            return
        }
        val oppgaverMedSvarEllerEldreEnn14Dager = oppgaveStatistikkService.oppgaverMedSvarEllerEldreEnn14Dager()
        bigQueryClient.publish(BIG_QUERY_DATASET, OppgaveSvartidTabell, oppgaverMedSvarEllerEldreEnn14Dager).also {
            loggPublisering(
                OppgaveSvartidTabell.tabellNavn,
                oppgaverMedSvarEllerEldreEnn14Dager.size
            )
        }
    }

    /**
     * Publiserer statistikk for inntektsraportering.
     */
    @Scheduled(cron = CRON_JOBB_HVER_TIME)
    fun publiserRapporterInntektStatistikk() {
        if (!leaderElectorService.erLeader()) {
            log.info("Denne instansen er ikke leder. Hopper over publisering av rapporter inntekt statistikk.")
            return
        }
        val sisteOppdateringAvTabell =
            bigQueryClient.finnSisteOppdateringAvTabell(BIG_QUERY_DATASET, RapporterInntektOppgaveTabell)
                ?: ZonedDateTime.of(Tid.TIDENES_BEGYNNELSE.atStartOfDay(), ZoneId.of("Europe/Oslo"))

        log.info("Henter oppgaver for rapporter inntekt som er oppdatert etter ${sisteOppdateringAvTabell}")


        val endredeOppgaver =
            oppgaveStatistikkService.oppgaverForRapporterInntektMedEndringSidenSisteKjøring(sisteOppdateringAvTabell)
        if (endredeOppgaver.isEmpty()) {
            log.info("Ingen endringer siden siste kjøring for tabell ${RapporterInntektOppgaveTabell.tabellNavn}. Hopper over publisering.")
            return
        }
        bigQueryClient.publish(BIG_QUERY_DATASET, RapporterInntektOppgaveTabell, endredeOppgaver)
            .also {
                loggPublisering(RapporterInntektOppgaveTabell.tabellNavn, endredeOppgaver.size)
            }
    }


    /**
     * Publiserer statistikk for avvik i inntektsrapportering.
     */
    @Scheduled(cron = CRON_JOBB_HVER_TIME)
    fun publiserBekreftAvvikStatistikk() {
        if (!leaderElectorService.erLeader()) {
            log.info("Denne instansen er ikke leder. Hopper over publisering av bekreft avvik statistikk.")
            return
        }
        val sisteOppdateringAvTabell =
            bigQueryClient.finnSisteOppdateringAvTabell(BIG_QUERY_DATASET, BekreftAvvikOppgaveTabell)
                ?: ZonedDateTime.of(Tid.TIDENES_BEGYNNELSE.atStartOfDay(), ZoneId.of("Europe/Oslo"))

        log.info("Henter oppgaver for bekreft avvik som er oppdatert etter $sisteOppdateringAvTabell")

        val endredeOppgaver =
            oppgaveStatistikkService.oppgaverForBekreftAvvikMedEndringSidenSisteKjøring(sisteOppdateringAvTabell)
        if (endredeOppgaver.isEmpty()) {
            log.info("Ingen endringer siden siste kjøring for tabell ${BekreftAvvikOppgaveTabell.tabellNavn}. Hopper over publisering.")
            return
        }
        bigQueryClient.publish(BIG_QUERY_DATASET, BekreftAvvikOppgaveTabell, endredeOppgaver)
            .also {
                loggPublisering(BekreftAvvikOppgaveTabell.tabellNavn, endredeOppgaver.size)
            }
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
