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
        val oppgaveId = UUID.fromString(oppgaveBekreftelse.søknadId.id)

        logger.info("Henter deltakelse for oppgaveId=$oppgaveId")
        val deltakelse = deltakelseRepository.finnDeltakelseGittOppgaveId(oppgaveId) ?: throw RuntimeException("Fant ikke deltakelse for oppgaveId=$oppgaveId")
        val oppgave = deltakelse.oppgaver.find { it.id == oppgaveId } ?: throw RuntimeException("Fant ikke oppgave for oppgaveId=$oppgaveId")

        logger.info("Markerer oppgave som løst for deltakelseId=${deltakelse.id}")
        val løstOppgave = oppgave.markerSomLøst()
        deltakelse.oppdaterOppgave(løstOppgave)

        deltakelseRepository.save(deltakelse)
        // TODO: Inaktivere oppgave på mine-sider.
    }
}
