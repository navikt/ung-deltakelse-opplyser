package no.nav.ung.deltakelseopplyser.domene.oppgave

import no.nav.familie.prosessering.internal.TaskService
import no.nav.k9.oppgave.bekreftelse.Bekreftelse
import no.nav.k9.oppgave.bekreftelse.ung.inntekt.InntektBekreftelse
import no.nav.k9.oppgave.bekreftelse.ung.periodeendring.EndretSluttdatoBekreftelse
import no.nav.k9.oppgave.bekreftelse.ung.periodeendring.EndretStartdatoBekreftelse
import no.nav.tms.varsel.action.Varseltype
import no.nav.ung.deltakelseopplyser.config.DeltakerappConfig
import no.nav.ung.deltakelseopplyser.config.TxConfiguration.Companion.TRANSACTION_MANAGER
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDAO
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.minside.DeaktiverVarselMinSideTask
import no.nav.ung.deltakelseopplyser.domene.minside.OpprettVarselMinSideTask
import no.nav.ung.deltakelseopplyser.domene.oppgave.kafka.UngdomsytelseOppgavebekreftelse
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.EndretSluttdatoOppgaveDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.EndretStartdatoOppgaveDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.InntektsrapporteringOppgavetypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.KontrollerRegisterInntektOppgaveTypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveBekreftelse
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveDAO
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
    private val deltakerappConfig: DeltakerappConfig,
    private val oppgaveMapperService: OppgaveMapperService,
    private val taskService: TaskService,
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
        oppgave.oppgaveBekreftelse = OppgaveBekreftelse(
            harUttalelse = bekreftelse.harUttalelse(),
            uttalelseFraBruker = bekreftelse.uttalelseFraBruker
        )

        deltakerService.oppdaterDeltaker(deltaker)

        logger.info("Deaktiverer oppgave med oppgaveReferanse=$oppgaveReferanse da den er løst")
        taskService.save(DeaktiverVarselMinSideTask.opprettTask(oppgave.oppgaveReferanse))
    }

    fun opprettOppgave(
        deltaker: DeltakerDAO,
        oppgaveReferanse: UUID,
        oppgaveTypeDataDAO: OppgavetypeDataDAO,
        frist: ZonedDateTime,
    ): OppgaveDTO {
        val oppgavetype = when (oppgaveTypeDataDAO) {
            is KontrollerRegisterInntektOppgaveTypeDataDAO -> Oppgavetype.BEKREFT_AVVIK_REGISTERINNTEKT
            is EndretStartdatoOppgaveDataDAO -> Oppgavetype.BEKREFT_ENDRET_STARTDATO
            is EndretSluttdatoOppgaveDataDAO -> Oppgavetype.BEKREFT_ENDRET_SLUTTDATO
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
            frist = frist,
            løstDato = null
        )

        logger.info("Legger til oppgave med id ${nyOppgave.id} på deltaker med id ${deltaker.id}")
        deltaker.leggTilOppgave(nyOppgave)
        deltakerService.oppdaterDeltaker(deltaker)

        taskService.save(
            OpprettVarselMinSideTask.opprettTask(
                OpprettVarselMinSideTask.OpprettVarselMinSideData(
                    varselId = nyOppgave.oppgaveReferanse.toString(),
                    deltakerIdent = deltaker.deltakerIdent,
                    tekster = oppgaveTypeDataDAO.minSideVarselTekster(),
                    varselLink = utledVarselLink(nyOppgave),
                    varseltype = Varseltype.Oppgave,
                    aktivFremTil = frist,
                )
            )
        )

        return oppgaveMapperService.mapOppgaveTilDTO(nyOppgave)
    }

    private fun utledVarselLink(nyOppgave: OppgaveDAO) =
        when (nyOppgave.oppgavetype) {
            Oppgavetype.SØK_YTELSE -> deltakerappConfig.getSøknadUrl()
            else -> deltakerappConfig.getOppgaveUrl(nyOppgave.oppgaveReferanse.toString())
        }

    fun avbrytOppgave(deltaker: DeltakerDAO, oppgaveReferanse: UUID): OppgaveDTO {
        logger.info("Henter oppgave med oppgaveReferanse $oppgaveReferanse")
        val oppgave = deltaker.oppgaver
            .find { it.oppgaveReferanse == oppgaveReferanse }!! // Deltaker ble funnet med samme oppgaveReferanse.

        if (oppgave.status == OppgaveStatus.LØST) {
            logger.error("Oppgave med oppgaveReferanse $oppgaveReferanse er løst og kan ikke avbrytes.")
            return oppgaveMapperService.mapOppgaveTilDTO(oppgave)
        }

        logger.info("Markerer oppgave med oppgaveReferanse $oppgaveReferanse som avbrutt")
        val oppdatertOppgave = oppgave.markerSomAvbrutt()

        logger.info("Lagrer oppgave med oppgaveReferanse $oppgaveReferanse på deltaker med id ${deltaker.id}")
        deltakerService.oppdaterDeltaker(deltaker)

        logger.info("Deaktiverer oppgave med oppgaveReferanse $oppgaveReferanse på min side")
        taskService.save(DeaktiverVarselMinSideTask.opprettTask(oppgave.oppgaveReferanse))

        return oppgaveMapperService.mapOppgaveTilDTO(oppdatertOppgave)
    }

    fun utløperOppgave(deltaker: DeltakerDAO, oppgaveReferanse: UUID): OppgaveDTO {
        logger.info("Henter oppgave med oppgaveReferanse $oppgaveReferanse")
        val oppgave = deltaker.oppgaver
            .find { it.oppgaveReferanse == oppgaveReferanse }!! // Deltaker ble funnet med samme oppgaveReferanse.

        if (oppgave.status == OppgaveStatus.LØST) {
            logger.error("Oppgave med oppgaveReferanse $oppgaveReferanse er løst og kan ikke utløpes.")
            return oppgaveMapperService.mapOppgaveTilDTO(oppgave)
        }

        logger.info("Markerer oppgave med oppgaveReferanse $oppgaveReferanse som utløpt")
        val oppdatertOppgave = oppgave.markerSomUtløpt()

        logger.info("Lagrer oppgave med oppgaveReferanse $oppgaveReferanse på deltaker med id ${deltaker.id}")
        deltakerService.oppdaterDeltaker(deltaker)

        logger.info("Deaktiverer oppgave med oppgaveReferanse $oppgaveReferanse på min side")
        taskService.save(DeaktiverVarselMinSideTask.opprettTask(oppgave.oppgaveReferanse))

        return oppgaveMapperService.mapOppgaveTilDTO(oppdatertOppgave)
    }

    fun løsOppgave(deltaker: DeltakerDAO, oppgaveReferanse: UUID?): OppgaveDTO {
        logger.info("Markerer oppgave som løst for deltaker=${deltaker.id}")

        val oppgave = deltaker.oppgaver
            .find { it.oppgaveReferanse == oppgaveReferanse }
            ?: throw RuntimeException("Deltaker har ikke oppgave for oppgaveReferanse=$oppgaveReferanse")

        val oppdatertOppgave = oppgave.markerSomLøst()

        logger.info("Lagrer oppgave med oppgaveReferanse $oppgaveReferanse på deltaker med id ${deltaker.id}")
        deltakerService.oppdaterDeltaker(deltaker)

        logger.info("Deaktiverer oppgave med oppgaveReferanse=$oppgaveReferanse da den er løst")
        taskService.save(DeaktiverVarselMinSideTask.opprettTask(oppgave.oppgaveReferanse))
        return oppgaveMapperService.mapOppgaveTilDTO(oppdatertOppgave)
    }

    fun løsOppgave(oppgaveReferanse: UUID): OppgaveDTO {
        val (deltaker, oppgave) = hentDeltakerOppgave(oppgaveReferanse)

        logger.info("Markerer oppgave som løst for deltaker=${deltaker.id}")
        val oppdatertOppgave = oppgave.markerSomLøst()

        logger.info("Lagrer oppgave med oppgaveReferanse $oppgaveReferanse på deltaker med id ${deltaker.id}")
        deltakerService.oppdaterDeltaker(deltaker)

        logger.info("Deaktiverer oppgave med oppgaveReferanse=$oppgaveReferanse da den er løst")
        taskService.save(DeaktiverVarselMinSideTask.opprettTask(oppgave.oppgaveReferanse))
        return oppgaveMapperService.mapOppgaveTilDTO(oppdatertOppgave)
    }

    fun lukkOppgave(oppgaveReferanse: UUID): OppgaveDTO {
        val OPPGAVER_SOM_STØTTER_Å_LUKKES = listOf(Oppgavetype.RAPPORTER_INNTEKT)

        logger.info("Henter oppgave med oppgaveReferanse $oppgaveReferanse")
        val (deltaker, oppgave) = hentDeltakerOppgave(oppgaveReferanse)

        if (!OPPGAVER_SOM_STØTTER_Å_LUKKES.contains(oppgave.oppgavetype)) {
            throw ErrorResponseException(
                HttpStatus.BAD_REQUEST,
                ProblemDetail.forStatusAndDetail(
                    HttpStatus.BAD_REQUEST,
                    "Oppgave med referanse $oppgaveReferanse kan kun lukkes dersom den er av type ${
                        OPPGAVER_SOM_STØTTER_Å_LUKKES.joinToString(
                            ","
                        )
                    }"
                ),
                null
            )
        }

        if (oppgave.status != OppgaveStatus.ULØST) {
            throw ErrorResponseException(
                HttpStatus.BAD_REQUEST,
                ProblemDetail.forStatusAndDetail(
                    HttpStatus.BAD_REQUEST,
                    "Oppgave med referanse $oppgaveReferanse kan kun lukkes dersom den er uløst."
                ),
                null
            )
        }

        val oppdatertOppgave = oppgave.markerSomLukket()
        deltakerService.oppdaterDeltaker(deltaker)
        taskService.save(DeaktiverVarselMinSideTask.opprettTask(oppgave.oppgaveReferanse))
        return oppgaveMapperService.mapOppgaveTilDTO(oppdatertOppgave)
    }

    fun åpneOppgave(oppgaveReferanse: UUID): OppgaveDTO {
        val (deltaker, oppgave) = hentDeltakerOppgave(oppgaveReferanse)

        val oppdatertOppgave = oppgave.markerSomÅpnet()
        deltakerService.oppdaterDeltaker(deltaker)
        return oppgaveMapperService.mapOppgaveTilDTO(oppdatertOppgave)
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

        Oppgavetype.BEKREFT_ENDRET_STARTDATO ->
            oppgaveBekreftelse.getBekreftelse() as? EndretStartdatoBekreftelse
                ?: throw IllegalStateException(
                    "For oppgavetype=${oppgave.oppgavetype} forventet EndretProgramperiodeBekreftelse, " +
                            "men fikk ${oppgaveBekreftelse.getBekreftelse<Bekreftelse>()::class.simpleName}"
                )


        Oppgavetype.BEKREFT_ENDRET_SLUTTDATO ->
            oppgaveBekreftelse.getBekreftelse() as? EndretSluttdatoBekreftelse
                ?: throw IllegalStateException(
                    "For oppgavetype=${oppgave.oppgavetype} forventet EndretProgramperiodeBekreftelse, " +
                            "men fikk ${oppgaveBekreftelse.getBekreftelse<Bekreftelse>()::class.simpleName}"
                )

        else -> throw IllegalStateException("Uventet oppgavetype=${oppgave.oppgavetype}")
    }

    private fun hentDeltakerOppgave(oppgaveReferanse: UUID): Pair<DeltakerDAO, OppgaveDAO> {
        val deltaker = (deltakerService.finnDeltakerGittOppgaveReferanse(oppgaveReferanse)
            ?: throw ErrorResponseException(
                HttpStatus.NOT_FOUND,
                ProblemDetail.forStatusAndDetail(
                    HttpStatus.NOT_FOUND,
                    "Fant ingen deltaker med oppgave referanse $oppgaveReferanse."
                ),
                null
            ))

        val oppgaveDAO = deltakerService.hentDeltakersOppgaver(deltaker.deltakerIdent)
            .find { it.oppgaveReferanse == oppgaveReferanse }!! // Funnet deltaker via oppgave referanse over.

        return Pair(deltaker, oppgaveDAO)
    }
}
