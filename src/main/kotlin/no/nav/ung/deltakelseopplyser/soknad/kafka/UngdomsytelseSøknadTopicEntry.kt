package no.nav.ung.deltakelseopplyser.soknad.kafka

import no.nav.k9.søknad.Søknad

data class MetaInfo(
    val version: Int = 1,
    val correlationId: String,
    val soknadDialogCommitSha: String? = null,
)

data class UngdomsytelseSøknadTopicEntry(val metadata: MetaInfo, val data: JournalførtUngdomsytelseSøknad)

data class JournalførtUngdomsytelseSøknad(
    val journalførtMelding: Ungdomsytelsesøknad,
)

data class Ungdomsytelsesøknad(
    val journalpostId: String,
    val søknad: Søknad
)
