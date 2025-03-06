package no.nav.ung.deltakelseopplyser.domene.oppgave.kafka

import com.fasterxml.jackson.annotation.JsonAlias
import no.nav.k9.oppgave.OppgaveBekreftelse
import no.nav.ung.deltakelseopplyser.domene.felles.MetaInfo

data class UngdomsytelseOppgavebekreftelseTopicEntry(val metadata: MetaInfo, val data: JournalførtUngdomsytelseOppgavebekreftelse)

data class JournalførtUngdomsytelseOppgavebekreftelse(
    val journalførtMelding: UngdomsytelseOppgavebekreftelse,
)

data class UngdomsytelseOppgavebekreftelse(
    val journalpostId: String,
    @JsonAlias("søknad") val oppgaveBekreftelse: OppgaveBekreftelse
)
