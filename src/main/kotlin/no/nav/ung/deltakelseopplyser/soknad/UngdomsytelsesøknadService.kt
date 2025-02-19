package no.nav.ung.deltakelseopplyser.soknad

import no.nav.k9.søknad.JsonUtils
import no.nav.ung.deltakelseopplyser.soknad.kafka.Ungdomsytelsesøknad
import no.nav.ung.deltakelseopplyser.soknad.repository.SøknadRepository
import no.nav.ung.deltakelseopplyser.soknad.repository.UngSøknadDAO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class UngdomsytelsesøknadService(
    private val søknadRepository: SøknadRepository
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(UngdomsytelsesøknadService::class.java)
    }

    fun håndterMottattSøknad(ungdomsytelsesøknad: Ungdomsytelsesøknad) {
        logger.info("Lagrer søknad med journalpostId: {}", ungdomsytelsesøknad.journalpostId)
        søknadRepository.save(ungdomsytelsesøknad.somUngSøknadDAO())

        // TODO: Hent tilhørende deltakelse for søknadens startdato
        // TODO: Marker deltakelse som søkt om den ikke er det
        // TODO: Marker deltakelsens relevante oppgave som løst hvis den har en endret startdato eller sluttdato
    }

    private fun Ungdomsytelsesøknad.somUngSøknadDAO(): UngSøknadDAO {
        return UngSøknadDAO(
            journalpostId = journalpostId,
            søkerIdent = søknad.søker.personIdent.verdi,
            søknad = JsonUtils.toString(søknad)
        )
    }
}
