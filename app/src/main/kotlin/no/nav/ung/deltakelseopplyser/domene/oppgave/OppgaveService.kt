package no.nav.ung.deltakelseopplyser.domene.oppgave

import no.nav.ung.deltakelseopplyser.domene.oppgave.kafka.UngdomsytelseOppgavebekreftelse
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramDeltakelseRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OppgaveService(
    private val deltakelseRepository: UngdomsprogramDeltakelseRepository
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(OppgaveService::class.java)
    }

    fun håndterMottattOppgavebekreftelse(ungdomsytelseOppgavebekreftelse: UngdomsytelseOppgavebekreftelse) {
        val oppgaveBekreftelse = ungdomsytelseOppgavebekreftelse.oppgaveBekreftelse
        val oppgaveReferanse = UUID.fromString(oppgaveBekreftelse.søknadId.id)

        logger.info("Henter deltakelse for oppgaveReferanse=$oppgaveReferanse")
        val deltakelse = deltakelseRepository.finnDeltakelseGittOppgaveReferanse(oppgaveReferanse) ?: throw RuntimeException("Fant ikke deltakelse for oppgaveReferanse=$oppgaveReferanse")
        val oppgave = deltakelse.oppgaver.find { it.oppgaveReferanse == oppgaveReferanse } ?: throw RuntimeException("Fant ikke oppgave for oppgaveReferanse=$oppgaveReferanse")

        logger.info("Markerer oppgave som løst for deltakelseId=${deltakelse.id}")
        val løstOppgave = oppgave.markerSomLøst()
        deltakelse.oppdaterOppgave(løstOppgave)

        deltakelseRepository.save(deltakelse)
        // TODO: Inaktivere oppgave på mine-sider.
    }
}
