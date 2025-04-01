package no.nav.ung.deltakelseopplyser.domene.register.ungsak

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.ung.deltakelseopplyser.config.Issuers
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDAO
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.KontrollerRegisterinntektOppgavetypeDataDTO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.*
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveDAO.Companion.tilDTO
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseOpplysningDTO
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramDeltakelseDAO
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramDeltakelseRepository
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService.Companion.mapToDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveStatus
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektOppgaveDTO

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.web.ErrorResponseException
import org.springframework.web.bind.annotation.*
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
    private val deltakerService: DeltakerService,
    private val deltakelseRepository: UngdomsprogramDeltakelseRepository,
) {
    @PostMapping("/avbryt", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Avbryter oppgave")
    @ResponseStatus(HttpStatus.OK)
    fun avbrytOppgave(@RequestBody oppgaveReferanse: UUID) {

        val deltaker =
            deltakerService.finnDeltakerGittOppgaveReferanse(oppgaveReferanse) ?: throw ErrorResponseException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR).also {
                    it.detail = "Fant ingen deltakelse med oppgave $oppgaveReferanse"
                },
                null
            )

        val oppgave = deltaker.oppgaver.find { it.oppgaveReferanse == oppgaveReferanse }
        oppgave!!.markerSomAvbrutt()
        deltaker.oppdaterOppgave(oppgave);
        deltakerService.oppdaterDeltaker(deltaker)
    }

    @PostMapping("/opprett", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Oppretter oppgave")
    @ResponseStatus(HttpStatus.OK)
    fun opprettOppgaveForKontrollAvRegisterinntekt(@RequestBody opprettOppgaveDto: RegisterInntektOppgaveDTO): DeltakelseOpplysningDTO {
        val deltaker = deltakerService.finnDeltakerGittIdent(opprettOppgaveDto.aktørId) ?: throw ErrorResponseException(
            HttpStatus.NOT_FOUND,
            ProblemDetail.forStatus(HttpStatus.NOT_FOUND).also {
                it.detail = "Fant ingen deltaker å opprette oppgave for"
            },
            null
        )
        val deltakersOppgaver = deltakerService.hentDeltakersOppgaver(opprettOppgaveDto.aktørId)

        val deltakelse = forsikreEksistererIProgram(deltaker)

        val harUløstOppgaveForSammePeriode = deltakersOppgaver.stream()
            .anyMatch {
                it.oppgavetype == Oppgavetype.BEKREFT_AVVIK_REGISTERINNTEKT &&
                        it.status == OppgaveStatus.ULØST &&
                        gjelderSammePeriode(it, opprettOppgaveDto)
            }

        if (harUløstOppgaveForSammePeriode) {
            throw IllegalStateException("Det finnes allerede en uløst oppgave for samme periode")
        }

        val nyOppgave = OppgaveDAO(
            id = UUID.randomUUID(),
            oppgaveReferanse = opprettOppgaveDto.referanse,
            deltaker = deltaker,
            oppgavetype = Oppgavetype.BEKREFT_AVVIK_REGISTERINNTEKT,
            oppgavetypeDataDAO = KontrollerRegisterInntektOppgaveTypeDataDAO(
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
            ),
            status = OppgaveStatus.ULØST,
            opprettetDato = ZonedDateTime.now(ZoneOffset.UTC),
            løstDato = null
        )

        deltaker.leggTilOppgave(nyOppgave)
        deltakerService.oppdaterDeltaker(deltaker)
        return deltakelse.mapToDTO()
    }

    private fun gjelderSammePeriode(it: OppgaveDAO, ny: RegisterInntektOppgaveDTO): Boolean {
        val eksisterende: KontrollerRegisterinntektOppgavetypeDataDTO =
            it.oppgavetypeDataDAO.tilDTO() as KontrollerRegisterinntektOppgavetypeDataDTO;
        return !eksisterende.fraOgMed.isAfter(ny.tomDato) && !eksisterende.tilOgMed.isBefore(ny.fomDato)
    }

    private fun forsikreEksistererIProgram(deltaker: DeltakerDAO): UngdomsprogramDeltakelseDAO {
        val deltakterIder = deltakerService.hentDeltakterIder(deltaker.deltakerIdent)
        val deltagelser = deltakelseRepository.findByDeltaker_IdIn(deltakterIder)

        if (deltagelser.isEmpty()) {
            throw ErrorResponseException(
                HttpStatus.NOT_FOUND,
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND).also {
                    it.detail = "Fant ingen deltakelse med id ${deltaker}"
                },
                null
            )
        }

        if (deltagelser.size > 1) {
            throw ErrorResponseException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR).also {
                    it.detail = "Fant flere deltakelser med id ${deltaker.id}"
                },
                null
            )
        }
        return deltagelser.first()
    }

}
