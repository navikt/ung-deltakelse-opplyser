package no.nav.ung.deltakelseopplyser.domene.oppgave

import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.oppgave.kafka.UngdomsytelseOppgavebekreftelse
import no.nav.ung.deltakelseopplyser.domene.varsler.MineSiderVarselService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class OppgaveService(
    private val deltakerService: DeltakerService,
    private val mineSiderVarselService: MineSiderVarselService
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(OppgaveService::class.java)
    }

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

        deltakerService.oppdaterDeltaker(deltaker)

        logger.info("Deaktiverer oppgave med oppgaveReferanse=$oppgaveReferanse da den er løst")
        mineSiderVarselService.deaktiverOppgave(oppgave.oppgaveReferanse.toString())
    }
}
