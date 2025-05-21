package no.nav.ung.deltakelseopplyser.domene.register.ungsak

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.transaction.Transactional
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.ung.deltakelseopplyser.config.DeltakerappConfig
import no.nav.ung.deltakelseopplyser.config.Issuers
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDAO
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.ArbeidOgFrilansRegisterInntektDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.EndretProgramperiodeOppgavetypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.InntektsrapporteringOppgavetypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.KontrollerRegisterInntektOppgaveTypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveDAO.Companion.tilDTO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgavetypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.ProgramperiodeDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.RegisterinntektDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.YtelseRegisterInntektDAO
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramDeltakelseRepository
import no.nav.ung.deltakelseopplyser.domene.varsler.MineSiderVarselService
import no.nav.ung.deltakelseopplyser.integration.abac.TilgangskontrollService
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.KontrollerRegisterinntektOppgavetypeDataDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveStatus
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.SettTilUtløptDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.inntektsrapportering.InntektsrapporteringOppgaveDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.periodeendring.EndretProgamperiodeOppgaveDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektOppgaveDTO
import no.nav.ung.deltakelseopplyser.utils.DateUtils.måned
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.web.ErrorResponseException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*


@RestController
@RequestMapping("/oppgave")
@RequiredIssuers(
    ProtectedWithClaims(issuer = Issuers.AZURE)
)
@Tag(
    name = "Oppretter og endrer på oppgaver",
    description = "API for å opprette, avbryte og sette oppgaver til utløpt. Er sikret med Azure."
)
class OppgaveUngSakController(
    private val tilgangskontrollService: TilgangskontrollService,
    private val deltakerService: DeltakerService,
    private val deltakelseRepository: UngdomsprogramDeltakelseRepository,
    private val mineSiderVarselService: MineSiderVarselService,
    private val deltakerappConfig: DeltakerappConfig,
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(OppgaveUngSakController::class.java)
    }

    @PostMapping("/avbryt", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Avbryter oppgave")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    fun avbrytOppgave(@RequestBody oppgaveReferanse: UUID) {
        tilgangskontrollService.krevSystemtilgang()
        logger.info("Avbryter oppgave med referanse $oppgaveReferanse")

        val deltaker = deltakerEksistererMedOppgaveReferanse(oppgaveReferanse)

        logger.info("Henter oppgave med oppgaveReferanse $oppgaveReferanse")
        val oppgave = deltaker.oppgaver
            .find { it.oppgaveReferanse == oppgaveReferanse }!! // Deltaker ble funnet med samme oppgaveReferanse.
            .also { forsikreOppgaveIkkeErLøst(it) }

        logger.info("Markerer oppgave med oppgaveReferanse $oppgaveReferanse som avbrutt")
        oppgave.markerSomAvbrutt()

        logger.info("Lagrer oppgave med oppgaveReferanse $oppgaveReferanse på deltaker med id ${deltaker.id}")
        deltakerService.oppdaterDeltaker(deltaker)

        logger.info("Deaktiverer oppgave med oppgaveReferanse $oppgaveReferanse på min side")
        mineSiderVarselService.deaktiverOppgave(oppgave.oppgaveReferanse.toString())
    }

    @PostMapping("/utløpt", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Setter oppgave til utløpt")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    fun utløperOppgave(@RequestBody oppgaveReferanse: UUID) {
        tilgangskontrollService.krevSystemtilgang()
        logger.info("Utløper oppgave med referanse $oppgaveReferanse")

        val deltaker = deltakerEksistererMedOppgaveReferanse(oppgaveReferanse)

        logger.info("Henter oppgave med oppgaveReferanse $oppgaveReferanse")
        val oppgave = deltaker.oppgaver
            .find { it.oppgaveReferanse == oppgaveReferanse }!! // Deltaker ble funnet med samme oppgaveReferanse.
            .also { forsikreOppgaveIkkeErLøst(it) }

        logger.info("Markerer oppgave med oppgaveReferanse $oppgaveReferanse som utløpt")
        oppgave.markerSomUtløpt()

        logger.info("Lagrer oppgave med oppgaveReferanse $oppgaveReferanse på deltaker med id ${deltaker.id}")
        deltakerService.oppdaterDeltaker(deltaker)

        logger.info("Deaktiverer oppgave med oppgaveReferanse $oppgaveReferanse på min side")
        mineSiderVarselService.deaktiverOppgave(oppgave.oppgaveReferanse.toString())
    }

    @PostMapping("/utløpt/forTypeOgPeriode", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Setter oppgave til utløpt for type og periode")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    fun utløperOppgaveForTypeOgPeriode(@RequestBody settTilUtløptDTO: SettTilUtløptDTO) {
        tilgangskontrollService.krevSystemtilgang()
        logger.info("Utløper oppgave av type: ${settTilUtløptDTO.oppgavetype} med periode [${settTilUtløptDTO.fomDato} - ${settTilUtløptDTO.tomDato}]")

        val deltaker = deltakerEksisterer(settTilUtløptDTO.deltakerIdent)

        logger.info("Henter oppgave av type ${settTilUtløptDTO.oppgavetype} med periode [${settTilUtløptDTO.fomDato} - ${settTilUtløptDTO.tomDato}]")
        // Ser kun på uløste oppgaver. Dersom oppgaven har en annen status blir denne stående
        // Grunnen til dette er at ung-sak slipper å sjekke på status på oppgaven før den kaller
        val uløstOppgaveISammePeriode = deltaker.oppgaver
            .filter { it.oppgavetype == settTilUtløptDTO.oppgavetype }
            .find {
                it.oppgavetype == settTilUtløptDTO.oppgavetype && gjelderSammePeriodeForInntektsrapportering(
                    it,
                    settTilUtløptDTO.fomDato,
                    settTilUtløptDTO.tomDato
                ) && it.status == OppgaveStatus.ULØST
            }

        if (uløstOppgaveISammePeriode != null) {
            logger.info("Markerer oppgave som utløpt")
            uløstOppgaveISammePeriode.markerSomUtløpt()

            logger.info("Lagrer oppgave på deltaker med id ${deltaker.id}")
            deltakerService.oppdaterDeltaker(deltaker)

            logger.info("Deaktiverer oppgave på min side")
            mineSiderVarselService.deaktiverOppgave(uløstOppgaveISammePeriode.oppgaveReferanse.toString())
        }
    }

    @PostMapping("/opprett/kontroll/registerinntekt", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Oppretter oppgave for kontroll av registerinntekt")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    fun opprettOppgaveForKontrollAvRegisterinntekt(@RequestBody opprettOppgaveDto: RegisterInntektOppgaveDTO): OppgaveDTO {
        tilgangskontrollService.krevSystemtilgang()
        logger.info("Oppretter oppgave for kontroll av registerinntekt")
        val deltaker = forsikreEksistererIProgram(opprettOppgaveDto.deltakerIdent)

        val deltakersOppgaver = deltakerService.hentDeltakersOppgaver(opprettOppgaveDto.deltakerIdent)

        deltakersOppgaver.stream()
            .anyMatch {
                it.oppgavetype == Oppgavetype.BEKREFT_AVVIK_REGISTERINNTEKT &&
                        it.status == OppgaveStatus.ULØST &&
                        gjelderSammePeriodeForKontroll(it, opprettOppgaveDto.fomDato, opprettOppgaveDto.tomDato)
            }
            .also { harUløstOppgaveForSammePeriode ->
                if (harUløstOppgaveForSammePeriode) {
                    logger.error("Det finnes allerede en uløst oppgave for samme periode")
                    throw ErrorResponseException(
                        HttpStatus.CONFLICT,
                        ProblemDetail.forStatusAndDetail(
                            HttpStatus.CONFLICT,
                            "Det finnes allerede en uløst oppgave for samme periode"
                        ),
                        null
                    )
                }
            }

        return opprettOppgave(
            deltaker = deltaker,
            oppgaveReferanse = opprettOppgaveDto.referanse,
            oppgaveTypeDataDAO = KontrollerRegisterInntektOppgaveTypeDataDAO(
                fomDato = opprettOppgaveDto.fomDato,
                tomDato = opprettOppgaveDto.tomDato,
                registerinntekt = RegisterinntektDAO(
                    opprettOppgaveDto.registerInntekter.registerinntekterForArbeidOgFrilans?.map {
                        ArbeidOgFrilansRegisterInntektDAO(
                            it.beløp,
                            it.arbeidsgiverIdent
                        )
                    } ?: emptyList(),
                    opprettOppgaveDto.registerInntekter.registerinntekterForYtelse?.map {
                        YtelseRegisterInntektDAO(
                            it.beløp,
                            it.ytelseType
                        )
                    } ?: emptyList(),
                )
            )
        )
    }

    @PostMapping("/opprett/endre/programperiode", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Oppretter oppgave for endret programperiode")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    fun opprettOppgaveForEndretProgramperiode(@RequestBody endretProgramperiodeOppgaveDTO: EndretProgamperiodeOppgaveDTO): OppgaveDTO {
        tilgangskontrollService.krevSystemtilgang()
        logger.info("Oppretter oppgave for endret programperiode med referanse ${endretProgramperiodeOppgaveDTO.oppgaveReferanse}")
        val deltaker = forsikreEksistererIProgram(endretProgramperiodeOppgaveDTO.deltakerIdent)

        val deltakersOppgaver = deltakerService.hentDeltakersOppgaver(endretProgramperiodeOppgaveDTO.deltakerIdent)

        deltakersOppgaver.stream()
            .anyMatch { it.oppgavetype == Oppgavetype.BEKREFT_ENDRET_PROGRAMPERIODE && it.status == OppgaveStatus.ULØST }
            .also { harUløstEndreStartdatoOppgave ->
                if (harUløstEndreStartdatoOppgave) {
                    logger.error("Det finnes allerede en uløst oppgave for endret programperiode")
                    throw ErrorResponseException(
                        HttpStatus.CONFLICT,
                        ProblemDetail.forStatusAndDetail(
                            HttpStatus.CONFLICT,
                            "Det finnes allerede en uløst oppgave for endret programperiode"
                        ),
                        null
                    )
                }
            }

        return opprettOppgave(
            deltaker = deltaker,
            oppgaveReferanse = endretProgramperiodeOppgaveDTO.oppgaveReferanse,
            oppgaveTypeDataDAO = EndretProgramperiodeOppgavetypeDataDAO(
                programperiode = ProgramperiodeDAO(
                    fomDato = endretProgramperiodeOppgaveDTO.programperiode.fomDato,
                    tomDato = endretProgramperiodeOppgaveDTO.programperiode.tomDato
                ),
                forrigeProgramperiode = endretProgramperiodeOppgaveDTO.forrigeProgramperiode?.let {
                    ProgramperiodeDAO(fomDato = it.fomDato, tomDato = it.tomDato)
                }
            )
        )
    }

    @PostMapping("/opprett/inntektsrapportering", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Oppretter oppgave for inntektsrapportering")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    fun opprettOppgaveForInntektsrapportering(@RequestBody opprettInntektsrapporteringOppgaveDTO: InntektsrapporteringOppgaveDTO): OppgaveDTO {
        tilgangskontrollService.krevSystemtilgang()
        logger.info("Oppretter oppgave for kontroll av registerinntekt")
        val deltaker = forsikreEksistererIProgram(opprettInntektsrapporteringOppgaveDTO.deltakerIdent)

        val deltakersOppgaver =
            deltakerService.hentDeltakersOppgaver(opprettInntektsrapporteringOppgaveDTO.deltakerIdent)

        val uløstOppgaveISammePeriode = deltakersOppgaver
            .firstOrNull {
                it.oppgavetype == Oppgavetype.RAPPORTER_INNTEKT &&
                        it.status == OppgaveStatus.ULØST &&
                        gjelderSammePeriodeForInntektsrapportering(
                            it,
                            opprettInntektsrapporteringOppgaveDTO.fomDato,
                            opprettInntektsrapporteringOppgaveDTO.tomDato
                        )
            }

        return if (uløstOppgaveISammePeriode != null) {
            logger.warn("Det finnes allerede en uløst oppgave for inntektsrapportering i perioden [${opprettInntektsrapporteringOppgaveDTO.fomDato} - ${opprettInntektsrapporteringOppgaveDTO.tomDato}]. Returnerer oppgave med id ${uløstOppgaveISammePeriode.id}")
            uløstOppgaveISammePeriode.tilDTO()
        } else opprettOppgave(
            deltaker = deltaker,
            oppgaveReferanse = opprettInntektsrapporteringOppgaveDTO.referanse,
            oppgaveTypeDataDAO = InntektsrapporteringOppgavetypeDataDAO(
                fomDato = opprettInntektsrapporteringOppgaveDTO.fomDato,
                tomDato = opprettInntektsrapporteringOppgaveDTO.tomDato
            )
        )

    }

    private fun opprettOppgave(
        deltaker: DeltakerDAO,
        oppgaveReferanse: UUID,
        oppgaveTypeDataDAO: OppgavetypeDataDAO,
    ): OppgaveDTO {
        val oppgavetype = when (oppgaveTypeDataDAO) {
            is KontrollerRegisterInntektOppgaveTypeDataDAO -> Oppgavetype.BEKREFT_AVVIK_REGISTERINNTEKT
            is EndretProgramperiodeOppgavetypeDataDAO -> Oppgavetype.BEKREFT_ENDRET_PROGRAMPERIODE
            is InntektsrapporteringOppgavetypeDataDAO -> Oppgavetype.RAPPORTER_INNTEKT
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

        mineSiderVarselService.opprettVarsel(
            varselId = nyOppgave.oppgaveReferanse.toString(),
            deltakerIdent = deltaker.deltakerIdent,
            tekster = oppgaveTypeDataDAO.minSideVarselTekster(),
            varselLink = deltakerappConfig.getOppgaveUrl(nyOppgave.oppgaveReferanse.toString()),
            varseltype = Varseltype.Oppgave
        )

        return nyOppgave.tilDTO()
    }

    private fun forsikreEksistererIProgram(deltakerIdent: String): DeltakerDAO {
        logger.info("Forsikrer at deltaker eksisterer i programmet")
        val deltaker = deltakerService.finnDeltakerGittIdent(deltakerIdent)
            ?: throw ErrorResponseException(
                HttpStatus.NOT_FOUND,
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND).also {
                    it.detail = "Fant ingen deltaker å opprette oppgave for"
                },
                null
            )

        logger.info("Henter deltakelseIder for deltaker med id ${deltaker.id}")
        val deltakterIder = deltakerService.hentDeltakterIder(deltaker.deltakerIdent)

        logger.info("Henter alle deltakelser for deltaker med ider ${deltakterIder.joinToString(",")}")
        val deltagelser = deltakelseRepository.findByDeltaker_IdIn(deltakterIder)

        if (deltagelser.isEmpty()) {
            throw ErrorResponseException(
                HttpStatus.NOT_FOUND,
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND).also {
                    it.detail = "Fant ingen deltakelse med id ${deltaker.id}"
                },
                null
            )
        }
        return deltaker
    }

    private fun gjelderSammePeriodeForKontroll(
        oppgaveDAO: OppgaveDAO,
        fom: LocalDate,
        tom: LocalDate,
    ): Boolean {
        val eksisterende: KontrollerRegisterinntektOppgavetypeDataDTO =
            oppgaveDAO.oppgavetypeDataDAO.tilDTO() as KontrollerRegisterinntektOppgavetypeDataDTO
        logger.info("Sjekker om oppgave med oppgaveReferanse ${oppgaveDAO.oppgaveReferanse} gjelder samme periode som ny oppgave. Eksisterende: [${eksisterende.fraOgMed}/${eksisterende.tilOgMed}], Ny: [$fom/$tom]")
        return !eksisterende.fraOgMed.isAfter(tom) && !eksisterende.tilOgMed.isBefore(fom)
    }

    private fun gjelderSammePeriodeForInntektsrapportering(
        oppgaveDAO: OppgaveDAO,
        fom: LocalDate,
        tom: LocalDate,
    ): Boolean {
        val eksisterende = oppgaveDAO.oppgavetypeDataDAO as InntektsrapporteringOppgavetypeDataDAO
        val eksisterendeFraOgMed = eksisterende.fomDato
        val eksisterendeTilOgMed = eksisterende.tomDato

        logger.info("Sjekker om oppgave med oppgaveReferanse ${oppgaveDAO.oppgaveReferanse} gjelder samme periode som ny oppgave. Eksisterende: [$eksisterendeFraOgMed/$eksisterendeTilOgMed], Ny: [$fom/$tom]")
        return !eksisterendeFraOgMed.isAfter(tom) && !eksisterendeTilOgMed.isBefore(fom)
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


    private fun deltakerEksistererMedOppgaveReferanse(oppgaveReferanse: UUID): DeltakerDAO {
        logger.info("Henter deltaker med oppgaveReferanse $oppgaveReferanse")
        val deltaker =
            deltakerService.finnDeltakerGittOppgaveReferanse(oppgaveReferanse) ?: throw ErrorResponseException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR).also {
                    it.detail = "Fant ingen deltaker med oppgave $oppgaveReferanse"
                },
                null
            )
        return deltaker
    }

    private fun deltakerEksisterer(deltakerIdent: String): DeltakerDAO {
        logger.info("Henter deltaker.")
        val deltaker =
            deltakerService.finnDeltakerGittIdent(deltakerIdent) ?: throw ErrorResponseException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR).also {
                    it.detail = "Fant ingen deltaker"
                },
                null
            )
        return deltaker
    }

    private fun OppgavetypeDataDAO.minSideVarselTekster(): List<Tekst> = when (this) {
        is KontrollerRegisterInntektOppgaveTypeDataDAO -> listOf(
            Tekst(
                tekst = "Du har fått en oppgave om å bekrefte inntekten din",
                spraakkode = "nb",
                default = true
            )
        )

        is EndretProgramperiodeOppgavetypeDataDAO -> listOf(
            Tekst(
                tekst = "Du har fått en oppgave om å bekrefte endret programperiode.",
                spraakkode = "nb",
                default = true
            )
        )

        is InntektsrapporteringOppgavetypeDataDAO -> listOf(
            Tekst(
                tekst = "Du har fått en oppgave om å registrere inntekten din for ${fomDato.måned()} dersom du har det.",
                spraakkode = "nb",
                default = true
            )
        )

    }
}
