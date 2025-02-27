package no.nav.ung.deltakelseopplyser.domene.oppgave.kafka

import no.nav.k9.søknad.Søknad
import no.nav.ung.deltakelseopplyser.domene.felles.MetaInfo

data class UngdomsytelseOppgavebekreftelseTopicEntry(val metadata: MetaInfo, val data: JournalførtUngdomsytelseOppgavebekreftelse)

data class JournalførtUngdomsytelseOppgavebekreftelse(
    val journalførtMelding: UngdomsytelseOppgavebekreftelse,
)

data class UngdomsytelseOppgavebekreftelse(
    val journalpostId: String,
    val søknad: Søknad // TODO: Bytt til riktig type fra k9-format når den er tilgjengelig.
)
