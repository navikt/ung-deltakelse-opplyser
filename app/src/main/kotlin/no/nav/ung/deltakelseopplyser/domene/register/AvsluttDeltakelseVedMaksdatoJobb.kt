package no.nav.ung.deltakelseopplyser.domene.register

import io.hypersistence.utils.hibernate.type.range.Range
import no.nav.ung.deltakelseopplyser.config.TxConfiguration.Companion.TRANSACTION_MANAGER
import no.nav.ung.deltakelseopplyser.integration.leader.LeaderElectorService
import no.nav.ung.deltakelseopplyser.integration.pdl.api.PdlService
import no.nav.ung.deltakelseopplyser.integration.ungsak.UngSakService
import no.nav.ung.sak.kontrakt.hendelser.HendelseDto
import no.nav.ung.sak.kontrakt.hendelser.HendelseInfo
import no.nav.ung.sak.kontrakt.hendelser.UngdomsprogramOpphørHendelse
import no.nav.ung.sak.typer.AktørId
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Schedulert jobb som setter sluttdato (opphørsdato) på deltakelser
 * der maksdato er nådd. Kjører daglig.
 *
 * Dette er scenario 4: naturlig avslutning.
 * Deltaker-appen setter selv opphørsdato når maksdato passeres.
 */
@Service
@Profile(value = ["prod-gcp", "dev-gcp"])
class AvsluttDeltakelseVedMaksdatoJobb(
    private val deltakelseRepository: DeltakelseRepository,
    private val pdlService: PdlService,
    private val ungSakService: UngSakService,
    private val leaderElectorService: LeaderElectorService,
) {

    companion object {
        private val log = LoggerFactory.getLogger(AvsluttDeltakelseVedMaksdatoJobb::class.java)
        private const val CRON_DAGLIG_KL_06 = "0 0 6 * * *"
    }

    /**
     * Finner alle aktive deltakelser der maksdato <= i dag og tilOgMed ikke er satt,
     * og setter sluttdato = maksdato. Sender opphørshendelse til ung-sak.
     */
    @Scheduled(cron = CRON_DAGLIG_KL_06)
    fun avsluttDeltakelserVedMaksdato() {
        if (!leaderElectorService.erLeader()) {
            log.info("Ikke leader, hopper over avslutning av deltakelser ved maksdato.")
            return
        }

        val iDag = LocalDate.now()
        log.info("Starter jobb for å avslutte deltakelser der maksdato <= $iDag")

        val deltakelser = deltakelseRepository.findAktiveDeltakelserMedMaksdatoPassert(iDag)
        log.info("Fant ${deltakelser.size} deltakelser som skal avsluttes.")

        deltakelser.forEach { deltakelse ->
            try {
                avsluttEnkeltDeltakelse(deltakelse)
            } catch (e: Exception) {
                log.error("Feil ved avslutning av deltakelse ${deltakelse.id}", e)
            }
        }

        log.info("Ferdig med avslutning av deltakelser ved maksdato. Behandlet ${deltakelser.size} stk.")
    }

    @Transactional(TRANSACTION_MANAGER)
    fun avsluttEnkeltDeltakelse(deltakelse: DeltakelseDAO) {
        val maksdato = deltakelse.maksDato
            ?: throw IllegalStateException("Deltakelse ${deltakelse.id} mangler maksdato")

        log.info("Avslutter deltakelse ${deltakelse.id} med sluttdato = maksdato = $maksdato")

        val nyPeriode = Range.closed(deltakelse.getFom(), maksdato)
        deltakelse.oppdaterPeriode(nyPeriode)
        deltakelseRepository.save(deltakelse)

        // Send hendelse til ung-sak
        sendOpphørsHendelseTilUngSak(deltakelse, maksdato)
    }

    private fun sendOpphørsHendelseTilUngSak(deltakelse: DeltakelseDAO, opphørsdato: LocalDate) {
        try {
            val aktørIder = pdlService.hentAktørIder(deltakelse.deltaker.deltakerIdent)
            val nåværendeAktørId = aktørIder.first { !it.historisk }.ident

            val hendelseInfo = HendelseInfo.Builder().medOpprettet(LocalDateTime.now())
            aktørIder.forEach { hendelseInfo.leggTilAktør(AktørId(it.ident)) }

            val hendelse = UngdomsprogramOpphørHendelse(hendelseInfo.build(), opphørsdato)
            ungSakService.sendInnHendelse(HendelseDto(hendelse, AktørId(nåværendeAktørId)))

            log.info("Sendt opphørshendelse til ung-sak for deltakelse ${deltakelse.id}")
        } catch (e: Exception) {
            log.error("Kunne ikke sende opphørshendelse til ung-sak for deltakelse ${deltakelse.id}", e)
        }
    }
}
