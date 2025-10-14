package no.nav.ung.deltakelseopplyser.domene.register.ungsak

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.ung.deltakelseopplyser.config.Issuers
import no.nav.ung.deltakelseopplyser.config.TxConfiguration.Companion.TRANSACTION_MANAGER
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDAO
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.oppgave.OppgaveMapperService
import no.nav.ung.deltakelseopplyser.domene.oppgave.OppgaveService
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.ArbeidOgFrilansRegisterInntektDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.EndretSluttdatoOppgaveDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.EndretStartdatoOppgaveDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.InntektsrapporteringOppgavetypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.KontrollerRegisterInntektOppgaveTypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.RegisterinntektDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.YtelseRegisterInntektDAO
import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseRepository
import no.nav.ung.deltakelseopplyser.integration.abac.TilgangskontrollService
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveStatus
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.SettTilUtløptDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.inntektsrapportering.InntektsrapporteringOppgaveDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektOppgaveDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretSluttdatoOppgaveDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretStartdatoOppgaveDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.søkytelse.SøkYtelseOppgaveDTO
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.transaction.annotation.Transactional
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
    private val deltakelseRepository: DeltakelseRepository,
    private val oppgaveMapperService: OppgaveMapperService,
    private val oppgaveService: OppgaveService,
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(OppgaveUngSakController::class.java)
    }

    @PostMapping("/avbryt", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Avbryter oppgave")
    @ResponseStatus(HttpStatus.OK)
    @Transactional(TRANSACTION_MANAGER)
    fun avbrytOppgave(@RequestBody oppgaveReferanse: UUID) {
        tilgangskontrollService.krevSystemtilgang()
        logger.info("Avbryter oppgave med referanse $oppgaveReferanse")

        val deltaker = deltakerEksistererMedOppgaveReferanse(oppgaveReferanse)
        oppgaveService.avbrytOppgave(
            deltaker = deltaker,
            oppgaveReferanse = oppgaveReferanse
        )
    }

    @PostMapping("/utlopt", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Setter oppgave til utløpt")
    @ResponseStatus(HttpStatus.OK)
    @Transactional(TRANSACTION_MANAGER)
    fun utløperOppgave(@RequestBody oppgaveReferanse: UUID) {
        tilgangskontrollService.krevSystemtilgang()
        logger.info("Utløper oppgave med referanse $oppgaveReferanse")

        val deltaker = deltakerEksistererMedOppgaveReferanse(oppgaveReferanse)

        oppgaveService.utløperOppgave(
            deltaker = deltaker,
            oppgaveReferanse = oppgaveReferanse
        )
    }

    @PostMapping("/utlopt/forTypeOgPeriode", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Setter oppgave til utløpt for type og periode")
    @ResponseStatus(HttpStatus.OK)
    @Transactional(TRANSACTION_MANAGER)
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
            oppgaveService.utløperOppgave(deltaker, uløstOppgaveISammePeriode.oppgaveReferanse)
        }
    }

    @PostMapping("/opprett/kontroll/registerinntekt", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Oppretter oppgave for kontroll av registerinntekt")
    @ResponseStatus(HttpStatus.OK)
    @Transactional(TRANSACTION_MANAGER)
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

        return oppgaveService.opprettOppgave(
            deltaker = deltaker,
            oppgaveReferanse = opprettOppgaveDto.referanse,
            frist = ZonedDateTime.of(opprettOppgaveDto.frist, ZoneOffset.UTC),
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

    @PostMapping("/opprett/endret-startdato", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Oppretter oppgave for endret startdato")
    @ResponseStatus(HttpStatus.OK)
    @Transactional(TRANSACTION_MANAGER)
    fun opprettOppgaveForEndretStartdato(@RequestBody endretStartdatoOppgaveDTO: EndretStartdatoOppgaveDTO): OppgaveDTO {
        tilgangskontrollService.krevSystemtilgang()
        logger.info("Oppretter oppgave for endret startdato med referanse ${endretStartdatoOppgaveDTO.oppgaveReferanse}")
        val deltaker = forsikreEksistererIProgram(endretStartdatoOppgaveDTO.deltakerIdent)

        val deltakersOppgaver = deltakerService.hentDeltakersOppgaver(endretStartdatoOppgaveDTO.deltakerIdent)

        deltakersOppgaver.stream()
            .anyMatch { it.oppgavetype == Oppgavetype.BEKREFT_ENDRET_STARTDATO && it.status == OppgaveStatus.ULØST }
            .also { harUløstEndreStartdatoOppgave ->
                if (harUløstEndreStartdatoOppgave) {
                    logger.error("Det finnes allerede en uløst oppgave for endret startdato")
                    throw ErrorResponseException(
                        HttpStatus.CONFLICT,
                        ProblemDetail.forStatusAndDetail(
                            HttpStatus.CONFLICT,
                            "Det finnes allerede en uløst oppgave for endret startdato"
                        ),
                        null
                    )
                }
            }

        return oppgaveService.opprettOppgave(
            deltaker = deltaker,
            frist = ZonedDateTime.of(endretStartdatoOppgaveDTO.frist, ZoneOffset.UTC),
            oppgaveReferanse = endretStartdatoOppgaveDTO.oppgaveReferanse,
            oppgaveTypeDataDAO = EndretStartdatoOppgaveDataDAO(
                nyStartdato = endretStartdatoOppgaveDTO.nyStartdato,
                forrigeStartdato = endretStartdatoOppgaveDTO.forrigeStartdato
            )
        )
    }


    @PostMapping("/opprett/endret-sluttdato", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Oppretter oppgave for endret sluttdato")
    @ResponseStatus(HttpStatus.OK)
    @Transactional(TRANSACTION_MANAGER)
    fun opprettOppgaveForEndretSluttdato(@RequestBody endretSluttdatoOppgaveDTO: EndretSluttdatoOppgaveDTO): OppgaveDTO {
        tilgangskontrollService.krevSystemtilgang()
        logger.info("Oppretter oppgave for endret startdato med referanse ${endretSluttdatoOppgaveDTO.oppgaveReferanse}")
        val deltaker = forsikreEksistererIProgram(endretSluttdatoOppgaveDTO.deltakerIdent)

        val deltakersOppgaver = deltakerService.hentDeltakersOppgaver(endretSluttdatoOppgaveDTO.deltakerIdent)

        deltakersOppgaver.stream()
            .anyMatch { it.oppgavetype == Oppgavetype.BEKREFT_ENDRET_SLUTTDATO && it.status == OppgaveStatus.ULØST }
            .also { harUløstEndreStartdatoOppgave ->
                if (harUløstEndreStartdatoOppgave) {
                    logger.error("Det finnes allerede en uløst oppgave for endret sluttdato")
                    throw ErrorResponseException(
                        HttpStatus.CONFLICT,
                        ProblemDetail.forStatusAndDetail(
                            HttpStatus.CONFLICT,
                            "Det finnes allerede en uløst oppgave for endret sluttdato"
                        ),
                        null
                    )
                }
            }

        return oppgaveService.opprettOppgave(
            deltaker = deltaker,
            frist = ZonedDateTime.of(endretSluttdatoOppgaveDTO.frist, ZoneOffset.UTC),
            oppgaveReferanse = endretSluttdatoOppgaveDTO.oppgaveReferanse,
            oppgaveTypeDataDAO = EndretSluttdatoOppgaveDataDAO(
                nySluttdato = endretSluttdatoOppgaveDTO.nySluttdato,
                forrigeSluttdato = endretSluttdatoOppgaveDTO.forrigeSluttdato
            )
        )
    }

    @PostMapping("/opprett/inntektsrapportering", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Oppretter oppgave for inntektsrapportering")
    @ResponseStatus(HttpStatus.OK)
    @Transactional(TRANSACTION_MANAGER)
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
            oppgaveMapperService.mapOppgaveTilDTO(uløstOppgaveISammePeriode)
        } else oppgaveService.opprettOppgave(
            deltaker = deltaker,
            frist = ZonedDateTime.of(opprettInntektsrapporteringOppgaveDTO.frist, ZoneOffset.UTC),
            oppgaveReferanse = opprettInntektsrapporteringOppgaveDTO.referanse,
            oppgaveTypeDataDAO = InntektsrapporteringOppgavetypeDataDAO(
                fomDato = opprettInntektsrapporteringOppgaveDTO.fomDato,
                tomDato = opprettInntektsrapporteringOppgaveDTO.tomDato
            )
        )

    }

    @PostMapping("/los/sokytelse", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Løser søk ytelse oppgave for deltaker med gitt ident")
    @ResponseStatus(HttpStatus.OK)
    @Transactional(TRANSACTION_MANAGER)
    fun løsOppgaveForSøkytelse(@RequestBody søkYtelseOppgaveDTO: SøkYtelseOppgaveDTO): OppgaveDTO {
        tilgangskontrollService.krevSystemtilgang()
        logger.info("Oppretter oppgave for kontroll av registerinntekt")
        val deltaker = forsikreEksistererIProgram(søkYtelseOppgaveDTO.deltakerIdent)

        val deltakersOppgaver =
            deltakerService.hentDeltakersOppgaver(søkYtelseOppgaveDTO.deltakerIdent)

        val søkYtelseOppgave = deltakersOppgaver
            .firstOrNull {
                it.oppgavetype == Oppgavetype.SØK_YTELSE
            }

        if (søkYtelseOppgave == null)
        {
            throw ErrorResponseException(
                HttpStatus.NOT_FOUND,
                ProblemDetail.forStatusAndDetail(
                    HttpStatus.NOT_FOUND,
                    "Fant ingen oppgave av type SØK_YTELSE for deltaker med id ${deltaker.id}"
                ),
                null
            )
        }

        when (søkYtelseOppgave.status) {
            OppgaveStatus.LØST -> logger.info("Oppgave av type SØK_YTELSE for deltaker med id ${deltaker.id} har allerede status LØST")
            OppgaveStatus.ULØST -> oppgaveService.løsOppgave(søkYtelseOppgave.oppgaveReferanse)
            else -> {
                throw ErrorResponseException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Oppgaven av type SØK_YTELSE for deltaker med id ${deltaker.id} har status ${søkYtelseOppgave.status}."
                    ),
                    null
                )
            }
        }
        return oppgaveMapperService.mapOppgaveTilDTO(søkYtelseOppgave)

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
        val eksisterende: KontrollerRegisterInntektOppgaveTypeDataDAO =
            oppgaveDAO.oppgavetypeDataDAO as KontrollerRegisterInntektOppgaveTypeDataDAO
        logger.info("Sjekker om oppgave med oppgaveReferanse ${oppgaveDAO.oppgaveReferanse} gjelder samme periode som ny oppgave. Eksisterende: [${eksisterende.fomDato}/${eksisterende.tomDato}], Ny: [$fom/$tom]")
        return !eksisterende.fomDato.isAfter(tom) && !eksisterende.tomDato.isBefore(fom)
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
}
