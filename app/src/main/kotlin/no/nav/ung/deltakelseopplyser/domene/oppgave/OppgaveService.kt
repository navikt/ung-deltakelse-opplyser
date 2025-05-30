package no.nav.ung.deltakelseopplyser.domene.oppgave

import no.nav.k9.oppgave.bekreftelse.Bekreftelse
import no.nav.k9.oppgave.bekreftelse.ung.inntekt.InntektBekreftelse
import no.nav.k9.oppgave.bekreftelse.ung.periodeendring.EndretProgramperiodeBekreftelse
import no.nav.tms.varsel.action.Varseltype
import no.nav.ung.deltakelseopplyser.config.DeltakerappConfig
import no.nav.ung.deltakelseopplyser.config.TxConfiguration.Companion.TRANSACTION_MANAGER
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDAO
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.minside.MineSiderService
import no.nav.ung.deltakelseopplyser.domene.oppgave.kafka.UngdomsytelseOppgavebekreftelse
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.EndretProgramperiodeOppgavetypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.InntektsrapporteringOppgavetypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.KontrollerRegisterInntektOppgaveTypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveDAO.Companion.tilDTO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgavetypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.SøkYtelseOppgavetypeDataDAO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveStatus
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.ErrorResponseException
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*
import no.nav.k9.oppgave.OppgaveBekreftelse as UngOppgaveBekreftelse

@Service
class OppgaveService(
    private val deltakerService: DeltakerService,
    private val mineSiderService: MineSiderService,
    private val deltakerappConfig: DeltakerappConfig,
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(OppgaveService::class.java)
    }

    @Transactional(TRANSACTION_MANAGER)
    fun håndterMottattOppgavebekreftelse(ungdomsytelseOppgavebekreftelse: UngdomsytelseOppgavebekreftelse) {
        val oppgaveBekreftelse = ungdomsytelseOppgavebekreftelse.oppgaveBekreftelse
        val oppgaveReferanse = UUID.fromString(oppgaveBekreftelse.søknadId.id)

        logger.info("Henter deltakers oppgave for oppgaveReferanse=$oppgaveReferanse")
        val deltakerIdent = oppgaveBekreftelse.søker.personIdent.verdi
        val deltaker =
            deltakerService.finnDeltakerGittIdent(deltakerIdent) ?: throw RuntimeException("Deltaker ikke funnet.")

        val oppgave = deltakerService.hentDeltakersOppgaver(deltaker.deltakerIdent)
            .find { it.oppgaveReferanse == oppgaveReferanse }
            ?: throw RuntimeException("Deltaker har ikke oppgave for oppgaveReferanse=$oppgaveReferanse")

        logger.info("Markerer oppgave som løst for deltaker=${deltaker.id}")
        oppgave.markerSomLøst()

        forsikreRiktigOppgaveBekreftelse(oppgave, oppgaveBekreftelse)
        val bekreftelse = oppgaveBekreftelse.getBekreftelse<Bekreftelse>()
        oppgave.oppgaveBekreftelse = no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveBekreftelse(
            harGodtattEndringen = bekreftelse.harBrukerGodtattEndringen(),
            uttalelseFraBruker = bekreftelse.uttalelseFraBruker
        )

        deltakerService.oppdaterDeltaker(deltaker)

        logger.info("Deaktiverer oppgave med oppgaveReferanse=$oppgaveReferanse da den er løst")
        mineSiderService.deaktiverOppgave(oppgave.oppgaveReferanse.toString())
    }

    fun opprettOppgave(
        deltaker: DeltakerDAO,
        oppgaveReferanse: UUID,
        oppgaveTypeDataDAO: OppgavetypeDataDAO,
        aktivFremTil: ZonedDateTime? = null
    ): OppgaveDTO {
        val oppgavetype = when (oppgaveTypeDataDAO) {
            is KontrollerRegisterInntektOppgaveTypeDataDAO -> Oppgavetype.BEKREFT_AVVIK_REGISTERINNTEKT
            is EndretProgramperiodeOppgavetypeDataDAO -> Oppgavetype.BEKREFT_ENDRET_PROGRAMPERIODE
            is InntektsrapporteringOppgavetypeDataDAO -> Oppgavetype.RAPPORTER_INNTEKT
            is SøkYtelseOppgavetypeDataDAO -> Oppgavetype.SØK_YTELSE
        }

        logger.info("Oppretter ny oppgave av oppgavetype $oppgavetype med referanse $oppgaveReferanse")

        val nyOppgave = OppgaveDAO(
            id = UUID.randomUUID(),
            oppgaveReferanse = oppgaveReferanse,
            deltaker = deltaker,
            oppgavetype = oppgavetype,
            oppgavetypeDataDAO = oppgaveTypeDataDAO,
            status = OppgaveStatus.ULØST,
            opprettetDato = ZonedDateTime.now(ZoneOffset.UTC),
            løstDato = null
        )

        logger.info("Legger til oppgave med id ${nyOppgave.id} på deltaker med id ${deltaker.id}")
        deltaker.leggTilOppgave(nyOppgave)
        deltakerService.oppdaterDeltaker(deltaker)

        mineSiderService.opprettVarsel(
            varselId = nyOppgave.oppgaveReferanse.toString(),
            deltakerIdent = deltaker.deltakerIdent,
            tekster = oppgaveTypeDataDAO.minSideVarselTekster(),
            varselLink = deltakerappConfig.getOppgaveUrl(nyOppgave.oppgaveReferanse.toString()),
            varseltype = Varseltype.Oppgave,
            aktivFremTil = aktivFremTil,
        )

        return nyOppgave.tilDTO()
    }

    fun avbrytOppgave(deltaker: DeltakerDAO, oppgaveReferanse: UUID) {
        logger.info("Henter oppgave med oppgaveReferanse $oppgaveReferanse")
        val oppgave = deltaker.oppgaver
            .find { it.oppgaveReferanse == oppgaveReferanse }!! // Deltaker ble funnet med samme oppgaveReferanse.
            .also { forsikreOppgaveIkkeErLøst(it) }

        logger.info("Markerer oppgave med oppgaveReferanse $oppgaveReferanse som avbrutt")
        oppgave.markerSomAvbrutt()

        logger.info("Lagrer oppgave med oppgaveReferanse $oppgaveReferanse på deltaker med id ${deltaker.id}")
        deltakerService.oppdaterDeltaker(deltaker)

        logger.info("Deaktiverer oppgave med oppgaveReferanse $oppgaveReferanse på min side")
        mineSiderService.deaktiverOppgave(oppgave.oppgaveReferanse.toString())
    }

    fun utløperOppgave(deltaker: DeltakerDAO, oppgaveReferanse: UUID) {
        logger.info("Henter oppgave med oppgaveReferanse $oppgaveReferanse")
        val oppgave = deltaker.oppgaver
            .find { it.oppgaveReferanse == oppgaveReferanse }!! // Deltaker ble funnet med samme oppgaveReferanse.
            .also { forsikreOppgaveIkkeErLøst(it) }

        logger.info("Markerer oppgave med oppgaveReferanse $oppgaveReferanse som utløpt")
        oppgave.markerSomUtløpt()

        logger.info("Lagrer oppgave med oppgaveReferanse $oppgaveReferanse på deltaker med id ${deltaker.id}")
        deltakerService.oppdaterDeltaker(deltaker)

        logger.info("Deaktiverer oppgave med oppgaveReferanse $oppgaveReferanse på min side")
        mineSiderService.deaktiverOppgave(oppgave.oppgaveReferanse.toString())
    }

    fun løsOppgave(deltaker: DeltakerDAO, oppgaveReferanse: UUID?) {
        logger.info("Markerer oppgave som løst for deltaker=${deltaker.id}")

        val oppgave = deltaker.oppgaver
            .find { it.oppgaveReferanse == oppgaveReferanse }
            ?: throw RuntimeException("Deltaker har ikke oppgave for oppgaveReferanse=$oppgaveReferanse")

        oppgave.markerSomLøst()

        logger.info("Lagrer oppgave med oppgaveReferanse $oppgaveReferanse på deltaker med id ${deltaker.id}")
        deltakerService.oppdaterDeltaker(deltaker)

        logger.info("Deaktiverer oppgave med oppgaveReferanse=$oppgaveReferanse da den er løst")
        mineSiderService.deaktiverOppgave(oppgave.oppgaveReferanse.toString())
    }

    private fun forsikreRiktigOppgaveBekreftelse(
        oppgave: OppgaveDAO,
        oppgaveBekreftelse: UngOppgaveBekreftelse,
    ) = when (oppgave.oppgavetype) {
        Oppgavetype.BEKREFT_AVVIK_REGISTERINNTEKT ->
            oppgaveBekreftelse.getBekreftelse() as? InntektBekreftelse
                ?: throw IllegalStateException(
                    "For oppgavetype=${oppgave.oppgavetype} forventet InntektBekreftelse, " +
                            "men fikk ${oppgaveBekreftelse::class.simpleName}"
                )

        Oppgavetype.BEKREFT_ENDRET_PROGRAMPERIODE ->
            oppgaveBekreftelse.getBekreftelse() as? EndretProgramperiodeBekreftelse
                ?: throw IllegalStateException(
                    "For oppgavetype=${oppgave.oppgavetype} forventet EndretProgramperiodeBekreftelse, " +
                            "men fikk ${oppgaveBekreftelse.getBekreftelse<Bekreftelse>()::class.simpleName}"
                )

        else -> throw IllegalStateException("Uventet oppgavetype=${oppgave.oppgavetype}")
    }

    private fun forsikreOppgaveIkkeErLøst(oppgave: OppgaveDAO) {
        if (oppgave.status == OppgaveStatus.LØST) {
            logger.error("Oppgave med oppgaveReferanse ${oppgave.oppgaveReferanse} er løst og kan ikke endres.")
            throw ErrorResponseException(
                HttpStatus.BAD_REQUEST,
                ProblemDetail.forStatusAndDetail(
                    HttpStatus.BAD_REQUEST,
                    "Oppgave med oppgaveReferanse ${oppgave.oppgaveReferanse} er løst og kan ikke endres."
                ),
                null
            )
        }
    }
}
