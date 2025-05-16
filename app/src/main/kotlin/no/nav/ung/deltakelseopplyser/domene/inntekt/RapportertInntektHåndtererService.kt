package no.nav.ung.deltakelseopplyser.domene.inntekt

import no.nav.k9.søknad.JsonUtils
import no.nav.k9.søknad.Søknad
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.inntekt.kafka.UngdomsytelseRapportertInntekt
import no.nav.ung.deltakelseopplyser.domene.inntekt.repository.RapportertInntektRepository
import no.nav.ung.deltakelseopplyser.domene.inntekt.repository.UngRapportertInntektDAO
import no.nav.ung.deltakelseopplyser.domene.varsler.MineSiderVarselService
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class RapportertInntektHåndtererService(
    private val rapportertInntektRepository: RapportertInntektRepository,
    private val deltakerService: DeltakerService,
    private val mineSiderVarselService: MineSiderVarselService
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(RapportertInntektHåndtererService::class.java)
    }

    fun håndterRapportertInntekt(rapportertInntektTopicEntry: UngdomsytelseRapportertInntekt) {
        logger.info("Håndterer mottatt rapportert inntekt.")
        val rapportertInntekt: Søknad = rapportertInntektTopicEntry.rapportertInntekt
        val deltakerIdent = rapportertInntekt.søker.personIdent.verdi
        val oppgaveReferanse = UUID.fromString(rapportertInntekt.søknadId.id)

        logger.info("Henter deltakerIder for søker oppgitt i rapportert inntekt")
        val deltaker = deltakerService.finnDeltakerGittIdent(deltakerIdent)
            ?: throw IllegalStateException("Fant ingen deltakere med ident oppgitt i rapportert inntekt")

        val oppgave = deltakerService.hentDeltakersOppgaver(deltakerIdent)
            .find { it.oppgaveReferanse == oppgaveReferanse && it.oppgavetype == Oppgavetype.RAPPORTER_INNTEKT }
            ?: throw RuntimeException("Deltaker har ikke oppgave for oppgaveReferanse=$oppgaveReferanse")

        logger.info("Markerer oppgave som løst for deltaker=${deltaker.id}")
        oppgave.markerSomLøst()

        logger.info("Deaktiverer oppgave med oppgaveReferanse=$oppgaveReferanse da den er løst")
        mineSiderVarselService.deaktiverOppgave(oppgave.oppgaveReferanse.toString())

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
