package no.nav.ung.deltakelseopplyser.domene.oppgave

import no.nav.k9.oppgave.OppgaveBekreftelse as UngOppgaveBekreftelse
import no.nav.k9.oppgave.bekreftelse.Bekreftelse
import no.nav.k9.oppgave.bekreftelse.ung.inntekt.InntektBekreftelse
import no.nav.k9.oppgave.bekreftelse.ung.periodeendring.EndretProgramperiodeBekreftelse
import no.nav.ung.deltakelseopplyser.config.TxConfiguration.Companion.TRANSACTION_MANAGER
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.oppgave.kafka.UngdomsytelseOppgavebekreftelse
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveDAO
import no.nav.ung.deltakelseopplyser.domene.varsler.MineSiderVarselService
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class OppgaveService(
    private val deltakerService: DeltakerService,
    private val mineSiderVarselService: MineSiderVarselService
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(OppgaveService::class.java)
    }

    @Transactional(TRANSACTION_MANAGER)
    fun håndterMottattOppgavebekreftelse(ungdomsytelseOppgavebekreftelse: UngdomsytelseOppgavebekreftelse) {
        val oppgaveBekreftelse = ungdomsytelseOppgavebekreftelse.oppgaveBekreftelse
        val oppgaveReferanse = UUID.fromString(oppgaveBekreftelse.søknadId.id)

        logger.info("Henter deltakers oppgave for oppgaveReferanse=$oppgaveReferanse")
        val deltakerIdent = oppgaveBekreftelse.søker.personIdent.verdi
        val deltaker =
            deltakerService.finnDeltakerGittIdent(deltakerIdent) ?: throw RuntimeException("Deltaker ikke funnet.")

        val oppgave = deltakerService.hentDeltakersOppgaver(deltaker.deltakerIdent)
            .find { it.oppgaveReferanse == oppgaveReferanse }
            ?: throw RuntimeException("Deltaker har ikke oppgave for oppgaveReferanse=$oppgaveReferanse")

        logger.info("Markerer oppgave som løst for deltaker=${deltaker.id}")
        oppgave.markerSomLøst()

        forsikreRiktigOppgaveBekreftelse(oppgave, oppgaveBekreftelse)
        val bekreftelse = oppgaveBekreftelse.getBekreftelse<Bekreftelse>()
        oppgave.oppgaveBekreftelse = no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveBekreftelse(
            harGodtattEndringen = bekreftelse.harBrukerGodtattEndringen(),
            uttalelseFraBruker = bekreftelse.uttalelseFraBruker
        )

        deltakerService.oppdaterDeltaker(deltaker)

        logger.info("Deaktiverer oppgave med oppgaveReferanse=$oppgaveReferanse da den er løst")
        mineSiderVarselService.deaktiverOppgave(oppgave.oppgaveReferanse.toString())
    }

    private fun forsikreRiktigOppgaveBekreftelse(
        oppgave: OppgaveDAO,
        oppgaveBekreftelse: UngOppgaveBekreftelse,
    ) = when (oppgave.oppgavetype) {
        Oppgavetype.BEKREFT_AVVIK_REGISTERINNTEKT ->
            oppgaveBekreftelse.getBekreftelse() as? InntektBekreftelse
                ?: throw IllegalStateException(
                    "For oppgavetype=${oppgave.oppgavetype} forventet InntektBekreftelse, " +
                            "men fikk ${oppgaveBekreftelse::class.simpleName}"
                )

        Oppgavetype.BEKREFT_ENDRET_PROGRAMPERIODE ->
            oppgaveBekreftelse.getBekreftelse() as? EndretProgramperiodeBekreftelse
                ?: throw IllegalStateException(
                    "For oppgavetype=${oppgave.oppgavetype} forventet EndretProgramperiodeBekreftelse, " +
                            "men fikk ${oppgaveBekreftelse.getBekreftelse<Bekreftelse>()::class.simpleName}"
                )

        else -> throw IllegalStateException("Uventet oppgavetype=${oppgave.oppgavetype}")
    }
}
