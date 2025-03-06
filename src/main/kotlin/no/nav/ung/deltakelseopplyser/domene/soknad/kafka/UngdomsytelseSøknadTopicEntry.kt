package no.nav.ung.deltakelseopplyser.domene.soknad.kafka

import no.nav.k9.søknad.Søknad
import no.nav.ung.deltakelseopplyser.domene.felles.MetaInfo

data class UngdomsytelseSøknadTopicEntry(val metadata: MetaInfo, val data: JournalførtUngdomsytelseSøknad)

data class JournalførtUngdomsytelseSøknad(
    val journalførtMelding: Ungdomsytelsesøknad,
)

data class Ungdomsytelsesøknad(
    val journalpostId: String,
    val søknad: Søknad
)
