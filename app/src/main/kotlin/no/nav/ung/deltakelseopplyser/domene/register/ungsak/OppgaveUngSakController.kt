package no.nav.ung.deltakelseopplyser.domene.register.ungsak

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.transaction.Transactional
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.tms.varsel.action.Tekst
import no.nav.ung.deltakelseopplyser.config.Issuers
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDAO
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.ArbeidOgFrilansRegisterInntektDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.EndretProgramperiodeOppgavetypeDataDAO
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
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.periodeendring.EndretProgamperiodeOppgaveDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektOppgaveDTO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.web.ErrorResponseException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
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
    @Value("\${UNGDOMSYTELSE_DELTAKER_BASE_URL}") private val deltakerAppBaseUrl: String
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

    @Deprecated("Bruk /opprett/kontroll/registerinntekt")
    @PostMapping("/opprett", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Oppretter oppgave")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    fun kontrollAvRegisterinntekt(@RequestBody opprettOppgaveDto: RegisterInntektOppgaveDTO): OppgaveDTO {
        tilgangskontrollService.krevSystemtilgang()
        val deltaker = forsikreEksistererIProgram(opprettOppgaveDto.deltakerIdent)

        val deltakersOppgaver = deltakerService.hentDeltakersOppgaver(opprettOppgaveDto.deltakerIdent)

        val harUløstOppgaveForSammePeriode = deltakersOppgaver.stream()
            .anyMatch {
                it.oppgavetype == Oppgavetype.BEKREFT_AVVIK_REGISTERINNTEKT &&
                        it.status == OppgaveStatus.ULØST &&
                        gjelderSammePeriode(it, opprettOppgaveDto)
            }

        if (harUløstOppgaveForSammePeriode) {
            throw IllegalStateException("Det finnes allerede en uløst oppgave for samme periode")
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
                        gjelderSammePeriode(it, opprettOppgaveDto)
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

    private fun opprettOppgave(
        deltaker: DeltakerDAO,
        oppgaveReferanse: UUID,
        oppgaveTypeDataDAO: OppgavetypeDataDAO,
    ): OppgaveDTO {
        val oppgavetype = when (oppgaveTypeDataDAO) {
            is KontrollerRegisterInntektOppgaveTypeDataDAO -> Oppgavetype.BEKREFT_AVVIK_REGISTERINNTEKT
            is EndretProgramperiodeOppgavetypeDataDAO -> Oppgavetype.BEKREFT_ENDRET_PROGRAMPERIODE
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

        mineSiderVarselService.opprettOppgave(
            oppgaveId = nyOppgave.oppgaveReferanse.toString(),
            deltakerIdent = deltaker.deltakerIdent,
            oppgavetekster = listOf(
                Tekst(
                    tekst = oppgavetype.mineSiderVarselTekst,
                    spraakkode = "nb",
                    default = true
                )
            ),
            oppgavelenke = opprettOppgaveLenke(nyOppgave)
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

    private fun gjelderSammePeriode(
        oppgaveDAO: OppgaveDAO,
        registerInntektOppgaveDTO: RegisterInntektOppgaveDTO,
    ): Boolean {
        val eksisterende: KontrollerRegisterinntektOppgavetypeDataDTO =
            oppgaveDAO.oppgavetypeDataDAO.tilDTO() as KontrollerRegisterinntektOppgavetypeDataDTO
        logger.info("Sjekker om oppgave med oppgaveReferanse ${oppgaveDAO.oppgaveReferanse} gjelder samme periode som ny oppgave. Eksisterende: [${eksisterende.fraOgMed}/${eksisterende.tilOgMed}], Ny: [${registerInntektOppgaveDTO.fomDato}/${registerInntektOppgaveDTO.tomDato}]")
        return !eksisterende.fraOgMed.isAfter(registerInntektOppgaveDTO.tomDato) && !eksisterende.tilOgMed.isBefore(
            registerInntektOppgaveDTO.fomDato
        )
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

    private fun opprettOppgaveLenke(opprettetOppgave: OppgaveDAO) = "$deltakerAppBaseUrl/oppgave/${opprettetOppgave.oppgaveReferanse}"
}
