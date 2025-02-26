package no.nav.ung.deltakelseopplyser.domene.soknad

import no.nav.k9.søknad.JsonUtils
import no.nav.k9.søknad.ytelse.ung.v1.Ungdomsytelse
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramDeltakelseRepository
import no.nav.ung.deltakelseopplyser.domene.soknad.kafka.Ungdomsytelsesøknad
import no.nav.ung.deltakelseopplyser.domene.soknad.repository.SøknadRepository
import no.nav.ung.deltakelseopplyser.domene.soknad.repository.UngSøknadDAO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class UngdomsytelsesøknadService(
    private val søknadRepository: SøknadRepository,
    private val deltakerService: DeltakerService,
    private val deltakelseRepository: UngdomsprogramDeltakelseRepository
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(UngdomsytelsesøknadService::class.java)
    }

    fun håndterMottattSøknad(ungdomsytelsesøknad: Ungdomsytelsesøknad) {
        logger.info("Håndterer mottatt søknad.")
        val søknad = ungdomsytelsesøknad.søknad
        val ungdomsytelse = søknad.getYtelse<Ungdomsytelse>()
        val søktFraDato = ungdomsytelse.søknadsperiode.fraOgMed
        val deltakerIdent = søknad.søker.personIdent.verdi

        logger.info("Henter deltakerIder for søker oppgitt i søknaden")
        val deltakterIder = deltakerService.hentDeltakterIder(deltakerIdent)
        if (deltakterIder.isEmpty()) {
            throw IllegalStateException("Fant ingen deltakere med ident oppgitt i søknaden")
        }

        logger.info("Henter deltakelse som starter $søktFraDato")
        val deltakelseDAO = deltakelseRepository.finnDeltakelseSomStarter(deltakterIder, søktFraDato)
            ?: throw IllegalStateException("Fant ingen deltakelse som starter $søktFraDato")

        if (deltakelseDAO.harSøkt.not()) {
            logger.info("Markerer deltakelse med id={} som søkt for.", deltakelseDAO.id)
            deltakelseDAO.markerSomHarSøkt()
            deltakelseRepository.save(deltakelseDAO)
        } else {
            logger.info("Deltakelse med id={} er allerede markert som søkt. Vurderer å løse oppgaver.", deltakelseDAO.id)
        }

        // TODO: Marker deltakelsens relevante oppgave som løst hvis den har en endret startdato eller sluttdato

        logger.info("Lagrer søknad med journalpostId: {}", ungdomsytelsesøknad.journalpostId)
        søknadRepository.save(ungdomsytelsesøknad.somUngSøknadDAO())
    }

    private fun Ungdomsytelsesøknad.somUngSøknadDAO(): UngSøknadDAO {
        return UngSøknadDAO(
            journalpostId = journalpostId,
            søkerIdent = søknad.søker.personIdent.verdi,
            søknad = JsonUtils.toString(søknad)
        )
    }
}
