package no.nav.ung.deltakelseopplyser.domene.inntekt

import no.nav.k9.søknad.JsonUtils
import no.nav.k9.søknad.Søknad
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.inntekt.kafka.UngdomsytelseRapportertInntekt
import no.nav.ung.deltakelseopplyser.domene.inntekt.repository.RapportertInntektRepository
import no.nav.ung.deltakelseopplyser.domene.inntekt.repository.UngRapportertInntektDAO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RapportertInntektHåndtererService(
    private val rapportertInntektRepository: RapportertInntektRepository,
    private val deltakerService: DeltakerService
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(RapportertInntektHåndtererService::class.java)
    }

    fun håndterRapportertInntekt(rapportertInntektTopicEntry: UngdomsytelseRapportertInntekt) {
        logger.info("Håndterer mottatt rapportert inntekt.")
        val rapportertInntekt: Søknad = rapportertInntektTopicEntry.rapportertInntekt
        val deltakerIdent = rapportertInntekt.søker.personIdent.verdi

        logger.info("Henter deltakerIder for søker oppgitt i rapportert inntekt")
        val deltakterIder = deltakerService.hentDeltakterIder(deltakerIdent)
        if (deltakterIder.isEmpty()) {
            throw IllegalStateException("Fant ingen deltakere med ident oppgitt i rapportert inntekt")
        }

        logger.info("Lagrer rapportert inntekt med journalpostId: {}", rapportertInntektTopicEntry.journalpostId)
        rapportertInntektRepository.save(rapportertInntektTopicEntry.somRapportertInntektDAO())
    }

    private fun UngdomsytelseRapportertInntekt.somRapportertInntektDAO(): UngRapportertInntektDAO {
        return UngRapportertInntektDAO(
            journalpostId = journalpostId,
            søkerIdent = rapportertInntekt.søker.personIdent.verdi,
            inntekt = JsonUtils.toString(rapportertInntekt)
        )
    }
}
