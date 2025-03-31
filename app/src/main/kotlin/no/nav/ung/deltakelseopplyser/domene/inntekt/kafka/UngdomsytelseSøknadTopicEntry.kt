package no.nav.ung.deltakelseopplyser.domene.inntekt.kafka

import com.fasterxml.jackson.annotation.JsonAlias
import no.nav.ung.deltakelseopplyser.domene.felles.MetaInfo
import no.nav.k9.søknad.Søknad as UngSøknad

data class UngdomsytelseRapportertInntektTopicEntry(
    val metadata: MetaInfo,
    val data: JournalførtUngdomsytelseRapportertInntekt,
)

data class JournalførtUngdomsytelseRapportertInntekt(
    val journalførtMelding: UngdomsytelseRapportertInntekt,
)

data class UngdomsytelseRapportertInntekt(
    val journalpostId: String,
    @JsonAlias("søknad") val rapportertInntekt: UngSøknad,
)
