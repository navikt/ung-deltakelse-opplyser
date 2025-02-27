package no.nav.ung.deltakelseopplyser.domene.oppgave

import no.nav.ung.deltakelseopplyser.domene.oppgave.kafka.UngdomsytelseOppgavebekreftelse
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramDeltakelseRepository
import org.springframework.stereotype.Service

@Service
class OppgaveService(
    private val deltakelseRepository: UngdomsprogramDeltakelseRepository
) {

    fun håndterMottattOppgavebekreftelse(oppgavebekreftelse: UngdomsytelseOppgavebekreftelse) {
        // TODO: hent deltakelse gitt oppgaveId
        //TODO: hent oppgaven og merker den som løst.
        // TODO: Lagre deltakelsen igjen.
    }
}
