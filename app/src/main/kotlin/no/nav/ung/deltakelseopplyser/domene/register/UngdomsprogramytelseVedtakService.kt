package no.nav.ung.deltakelseopplyser.domene.register

import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.oppgave.OppgaveMapperService
import no.nav.ung.deltakelseopplyser.domene.oppgave.OppgaveService
import no.nav.ung.deltakelseopplyser.integration.pdl.api.PdlService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class UngdomsprogramytelseVedtakService(
    private val deltakelseRepository: DeltakelseRepository,
    private val deltakerService: DeltakerService,
    private val ungdomsprogramregisterService: UngdomsprogramregisterService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(UngdomsprogramytelseVedtakService::class.java)
    }

    fun håndterUngdomsprogramytelseOpphørsvedtakForAktør(aktørId: String) {
        val deltakerIder = deltakerService.hentDeltakterIder(aktørId)
        val slettetDeltakelser = deltakelseRepository.findByDeltaker_IdInAndErSlettet(deltakerIder, true)
        if (slettetDeltakelser.size > 1) {
            logger.warn("Flere (${slettetDeltakelser.size}) slettede deltakelser funnet for deltakelser=${slettetDeltakelser.map { it.id }.toList()}. Dette er uventet.")
        }
        slettetDeltakelser.forEach { ungdomsprogramregisterService.markerSomFattetOpphørsvedtak(it.id) }
    }
}
