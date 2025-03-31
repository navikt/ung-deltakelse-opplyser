package no.nav.ung.deltakelseopplyser.domene.register.ungsak

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.ung.deltakelseopplyser.config.Issuers
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

        val deltakelse =
            deltakelseRepository.finnDeltakelseGittOppgaveReferanse(oppgaveReferanse) ?: throw ErrorResponseException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR).also {
                    it.detail = "Fant ingen deltakelse med oppgave $oppgaveReferanse"
                },
                null
            )

        val oppgave = deltakelse.oppgaver.find { it.oppgaveReferanse == oppgaveReferanse }
        oppgave!!.markerSomAvbrutt()
        deltakelse.oppdaterOppgave(oppgave);
        deltakelseRepository.save(deltakelse)
    }

    @PostMapping("/opprett", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Oppretter oppgave")
    @ResponseStatus(HttpStatus.OK)
    fun opprettOppgaveForKontrollAvRegisterinntekt(@RequestBody opprettOppgaveDto: RegisterInntektOppgaveDTO): DeltakelseOpplysningDTO {
        val hentDeltakterIder = deltakerService.hentDeltakterIder(opprettOppgaveDto.aktørId)
        if (hentDeltakterIder.size > 1) {
            throw IllegalStateException("Fant flere deltakelser for samme id")
        }
        if (hentDeltakterIder.size == 0) {
            throw IllegalStateException("Fant ingen deltakelser for id")
        }

        val eksisterende = forsikreEksistererIProgram(hentDeltakterIder)

        val harUløstOppgaveForSammePeriode = eksisterende.oppgaver.stream()
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
            deltakelse = eksisterende,
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

        eksisterende.leggTilOppgave(nyOppgave)
        val oppdatertDeltakelse = deltakelseRepository.save(eksisterende)
        return oppdatertDeltakelse.mapToDTO()
    }

    private fun gjelderSammePeriode(it: OppgaveDAO, ny: RegisterInntektOppgaveDTO): Boolean {
        val eksisterende: KontrollerRegisterinntektOppgavetypeDataDTO =
            it.oppgavetypeDataDAO.tilDTO() as KontrollerRegisterinntektOppgavetypeDataDTO;
        return !eksisterende.fraOgMed.isAfter(ny.tomDato) && !eksisterende.tilOgMed.isBefore(ny.fomDato)
    }

    private fun forsikreEksistererIProgram(hentDeltakterIder: List<UUID>): UngdomsprogramDeltakelseDAO {
        val deltagelser = deltakelseRepository.findByDeltaker_IdIn(hentDeltakterIder)

        if (deltagelser.isEmpty()) {
            throw ErrorResponseException(
                HttpStatus.NOT_FOUND,
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND).also {
                    it.detail = "Fant ingen deltakelse med id ${hentDeltakterIder}"
                },
                null
            )
        }

        if (deltagelser.size > 1) {
            throw ErrorResponseException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR).also {
                    it.detail = "Fant flere deltakelser med id ${hentDeltakterIder}"
                },
                null
            )
        }
        return deltagelser.first()
    }

}
