package no.nav.ung.deltakelseopplyser.domene.oppgave

import no.nav.ung.deltakelseopplyser.domene.oppgave.kafka.UngdomsytelseOppgavebekreftelse
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramDeltakelseRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OppgaveService(
    private val deltakelseRepository: UngdomsprogramDeltakelseRepository
) {

    fun håndterMottattOppgavebekreftelse(ungdomsytelseOppgavebekreftelse: UngdomsytelseOppgavebekreftelse) {
        val oppgaveBekreftelse = ungdomsytelseOppgavebekreftelse.oppgaveBekreftelse
        val oppgaveId = UUID.fromString(oppgaveBekreftelse.søknadId.id)

        val deltakelse = deltakelseRepository.finnDeltakelseGittOppgaveId(oppgaveId) ?: throw RuntimeException("Fant ikke deltakelse for oppgaveId=$oppgaveId")
        val oppgave = deltakelse.oppgaver.find { it.id == oppgaveId } ?: throw RuntimeException("Fant ikke oppgave for oppgaveId=$oppgaveId")

        val løstOppgave = oppgave.markerSomLøst()
        deltakelse.oppdaterOppgave(løstOppgave)

        deltakelseRepository.save(deltakelse)
        // TODO: Inaktivere oppgave på mine-sider.
    }
}
