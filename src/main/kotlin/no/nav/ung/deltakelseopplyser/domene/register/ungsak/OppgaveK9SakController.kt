package no.nav.ung.deltakelseopplyser.domene.register.ungsak

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.ung.deltakelseopplyser.config.Issuers
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.oppgave.KontrollerRegisterinntektOppgavetypeDataDTO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.*
import no.nav.ung.deltakelseopplyser.domene.oppgave.tilDTO
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramDeltakelseDAO
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramDeltakelseRepository
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
    name = "Les register data",
    description = "API for å hente deltakelser for en gitt deltaker i ungdomsprogrammet. Er sikret med Azure."
)
class OppgaveK9SakController(
    private val deltakerService: DeltakerService,
    private val deltakelseRepository: UngdomsprogramDeltakelseRepository,
    ) {

    @PostMapping("/opprett", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Oppretter oppgave")
    @ResponseStatus(HttpStatus.OK)
    fun opprettOppgaveForKontrollAvRegisterinntekt(@RequestBody opprettOppgaveDto: RegisterInntektOppgaveDTO): UngdomsprogramDeltakelseDAO {
        val hentDeltakterIder = deltakerService.hentDeltakterIder(opprettOppgaveDto.aktørId)
        if (hentDeltakterIder.size > 1) {
            throw IllegalStateException("Fant flere deltakelser for samme id")
        }
        if (hentDeltakterIder.size == 0) {
            throw IllegalStateException("Fant ingen deltakelser for id")
        }

        val eksisterende = forsikreEksistererIProgram(hentDeltakterIder.get(0))

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
            deltakelse = eksisterende,
            oppgavetype = Oppgavetype.BEKREFT_AVVIK_REGISTERINNTEKT,
            oppgavetypeDataDAO = KontrollerRegisterInntektOppgaveTypeDataDAO(
                fomDato = opprettOppgaveDto.fomDato,
                tomDato = opprettOppgaveDto.tomDato,
                registerinntekt = RegisterinntektDAO(
                    opprettOppgaveDto.registerInntekter.registerinntekterForArbeidOgFrilans?.map { ArbeidOgFrilansRegisterInntektDAO(it.beløp, it.arbeidsgiverIdent) } ?: emptyList(),
                    opprettOppgaveDto.registerInntekter.registerinntekterForYtelse?.map { YtelseRegisterInntektDAO(it.beløp, it.ytelseType) } ?: emptyList(),
                )
            ),
            status = OppgaveStatus.ULØST,
            opprettetDato = ZonedDateTime.now(ZoneOffset.UTC),
            løstDato = null
        )

        eksisterende.leggTilOppgave(nyOppgave)
        val oppdatertDeltakelse = deltakelseRepository.save(eksisterende)
        return oppdatertDeltakelse;
    }

    private fun gjelderSammePeriode(it: OppgaveDAO, ny: RegisterInntektOppgaveDTO) : Boolean {
        val eksisterende :KontrollerRegisterinntektOppgavetypeDataDTO = it.oppgavetypeDataDAO.tilDTO() as KontrollerRegisterinntektOppgavetypeDataDTO;
        return !eksisterende.fomDato.isAfter(ny.tomDato) && !eksisterende.tomDato.isBefore(ny.fomDato)
    }


    private fun forsikreEksistererIProgram(id: UUID): UngdomsprogramDeltakelseDAO =
        deltakelseRepository.findById(id).orElseThrow {
            ErrorResponseException(
                HttpStatus.NOT_FOUND,
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND).also {
                    it.detail = "Fant ingen deltakelse med id $id"
                },
                null
            )
        }

}
