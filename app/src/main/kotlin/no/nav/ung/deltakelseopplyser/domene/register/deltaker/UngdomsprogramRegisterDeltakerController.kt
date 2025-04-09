package no.nav.ung.deltakelseopplyser.domene.register.deltaker

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.ung.deltakelseopplyser.config.Issuers.TOKEN_X
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveDAO.Companion.tilDTO
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakelsePeriodInfo
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveDTO
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseOpplysningDTO
import no.nav.ung.deltakelseopplyser.utils.personIdent
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.web.ErrorResponseException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/deltakelse/register")
@RequiredIssuers(
    ProtectedWithClaims(
        issuer = TOKEN_X,
        claimMap = ["acr=Level4", "acr=idporten-loa-high"],
        combineWithOr = true
    )
)
@Tag(name = "Deltakelse", description = "API for å hente opplysninger om deltakelse i ungdomsprogrammet")
class UngdomsprogramRegisterDeltakerController(
    private val registerService: UngdomsprogramregisterService,
    private val deltakerService: DeltakerService,
    private val tokenValidationContextHolder: SpringTokenValidationContextHolder,
) {

    @GetMapping("/hent/alle", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Henter alle deltakelser for en deltaker i ungdomsprogrammet")
    @ResponseStatus(HttpStatus.OK)
    fun hentAlleMineDeltakelser(): List<DeltakelsePeriodInfo> {
        val personIdent = tokenValidationContextHolder.personIdent()
        return registerService.hentAlleDeltakelsePerioderForDeltaker(deltakerIdentEllerAktørId = personIdent)
    }

    @PutMapping("/{id}/marker-har-sokt", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Markerer at deltakelsen er søkt om")
    @ResponseStatus(HttpStatus.OK)
    fun markerDeltakelseSomSøkt(@PathVariable id: UUID): DeltakelseOpplysningDTO {
        val alleDeltakersIdenter = deltakerService.hentDeltakterIdenter(tokenValidationContextHolder.personIdent())
        val personPåDeltakelsen = registerService.hentFraProgram(id).deltaker.deltakerIdent
        if (!alleDeltakersIdenter.contains(personPåDeltakelsen)) {
            throw ErrorResponseException(
                HttpStatus.FORBIDDEN,
                ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Bruker kan kun endre på egne data"),
                null
            )
        }

        return registerService.markerSomHarSøkt(id)
    }

    @GetMapping("/oppgave/{oppgaveReferanse}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Henter en oppgave for en gitt deltakelse")
    @ResponseStatus(HttpStatus.OK)
    fun hentDeltakersOppgave(@PathVariable oppgaveReferanse: UUID): OppgaveDTO {
        val personIdent = tokenValidationContextHolder.personIdent()
        return deltakerService.hentDeltakersOppgaver(personIdent)
            .find { it.oppgaveReferanse == oppgaveReferanse }?.tilDTO()
            ?: throw ErrorResponseException(
                HttpStatus.NOT_FOUND,
                ProblemDetail.forStatusAndDetail(
                    HttpStatus.NOT_FOUND,
                    "Fant ingen oppgave med referanse $oppgaveReferanse for deltaker."
                ),
                null
            )
    }
}
