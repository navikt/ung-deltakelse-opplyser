package no.nav.ung.deltakelseopplyser.soknad

import no.nav.k9.søknad.Søknad

data class MetaInfo(
    val version: Int = 1,
    val correlationId: String,
    val soknadDialogCommitSha: String? = null,
)

data class UngdomsytelseSøknadTopicEntry(val metadata: MetaInfo, val data: UngdomsytelseSøknad)

data class UngdomsytelseSøknad(
    val journalførtMelding: JournalførtUngdomsytelseSøknad,
)

data class JournalførtUngdomsytelseSøknad(
    val type: String,
    val journalpostId: String,
    val søknad: Søknad
)
