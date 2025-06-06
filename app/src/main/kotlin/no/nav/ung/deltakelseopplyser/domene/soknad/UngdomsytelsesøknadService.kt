package no.nav.ung.deltakelseopplyser.domene.soknad

import no.nav.k9.søknad.JsonUtils
import no.nav.k9.søknad.ytelse.ung.v1.Ungdomsytelse
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.minside.MineSiderService
import no.nav.ung.deltakelseopplyser.domene.minside.mikrofrontend.MicrofrontendService
import no.nav.ung.deltakelseopplyser.domene.minside.mikrofrontend.MicrofrontendStatus
import no.nav.ung.deltakelseopplyser.domene.minside.mikrofrontend.MinSideMicrofrontendStatusDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.OppgaveService
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramDeltakelseRepository
import no.nav.ung.deltakelseopplyser.domene.soknad.kafka.Ungdomsytelsesøknad
import no.nav.ung.deltakelseopplyser.domene.soknad.repository.SøknadRepository
import no.nav.ung.deltakelseopplyser.domene.soknad.repository.UngSøknadDAO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

@Service
class UngdomsytelsesøknadService(
    private val søknadRepository: SøknadRepository,
    private val deltakerService: DeltakerService,
    private val deltakelseRepository: UngdomsprogramDeltakelseRepository,
    private val microfrontendService: MicrofrontendService,
    private val oppgaveService: OppgaveService,
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(UngdomsytelsesøknadService::class.java)
    }

    fun håndterMottattSøknad(ungdomsytelsesøknad: Ungdomsytelsesøknad) {
        logger.info("Håndterer mottatt søknad.")
        val søknad = ungdomsytelsesøknad.søknad
        val oppgaveReferanse = UUID.fromString(søknad.søknadId.id)
        val ungdomsytelse = søknad.getYtelse<Ungdomsytelse>()
        val søktFraDato = ungdomsytelse.søknadsperiode.fraOgMed
        val deltakerIdent = søknad.søker.personIdent.verdi

        val deltakterIder = deltakerService.hentDeltakterIder(deltakerIdent)
        if (deltakterIder.isEmpty()) {
            throw IllegalStateException("Fant ingen deltakere med ident oppgitt i søknaden")
        }

        val sendSøknadOppgave = deltakerService.hentDeltakersOppgaver(deltakerIdent)
            .find { it.oppgaveReferanse == oppgaveReferanse && it.oppgavetype == Oppgavetype.SØK_YTELSE }
            ?: throw IllegalStateException("Fant ingen deltakere med ident oppgitt i søknaden som har oppgave for oppgaveReferanse=$oppgaveReferanse")

        logger.info("Henter deltakelse som starter $søktFraDato")
        val deltakelseDAO = deltakelseRepository.finnDeltakelseSomStarter(deltakterIder, søktFraDato)
            ?: throw IllegalStateException("Fant ingen deltakelse som starter $søktFraDato for deltaker med id=$deltakterIder")

        val deltaker = deltakerService.finnDeltakerGittIdent(deltakerIdent)
            ?: throw IllegalStateException("Fant ingen deltakere med ident oppgitt i søknaden")

        oppgaveService.løsOppgave(
            deltaker = deltaker,
            oppgaveReferanse = sendSøknadOppgave.oppgaveReferanse
        )

        if (deltakelseDAO.søktTidspunkt == null) {
            logger.info("Markerer deltakelse med id={} som søkt for.", deltakelseDAO.id)
            deltakelseDAO.markerSomHarSøkt()
            deltakelseRepository.save(deltakelseDAO)
        } else {
            logger.info(
                "Deltakelse med id={} er allerede markert som søkt. Vurderer å løse oppgaver.",
                deltakelseDAO.id
            )
        }

        logger.info("Lagrer søknad med journalpostId: {}", ungdomsytelsesøknad.journalpostId)
        søknadRepository.save(ungdomsytelsesøknad.somUngSøknadDAO())

        logger.info("Aktiverer mikrofrontend for deltaker med deltakelseId: {}", deltakelseDAO.id)
        microfrontendService.sendOgLagre(
            MinSideMicrofrontendStatusDAO(
                id = UUID.randomUUID(),
                deltaker = deltakelseDAO.deltaker,
                status = MicrofrontendStatus.ENABLE,
                opprettet = ZonedDateTime.now(ZoneOffset.UTC),
            )
        ).also {
            logger.info("Mikrofrontend aktivert for deltaker med deltakelseId: {}", deltakelseDAO.id)
        }
    }

    private fun Ungdomsytelsesøknad.somUngSøknadDAO(): UngSøknadDAO {
        return UngSøknadDAO(
            journalpostId = journalpostId,
            søkerIdent = søknad.søker.personIdent.verdi,
            søknad = JsonUtils.toString(søknad)
        )
    }
}
