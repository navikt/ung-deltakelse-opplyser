package no.nav.ung.deltakelseopplyser.domene.register.deltaker

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.ung.deltakelseopplyser.config.Issuers.TOKEN_X
import no.nav.ung.deltakelseopplyser.config.TxConfiguration.Companion.TRANSACTION_MANAGER
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.oppgave.OppgaveMapperService
import no.nav.ung.deltakelseopplyser.domene.oppgave.OppgaveService
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService
import no.nav.ung.deltakelseopplyser.integration.ungsak.UngOppgaverService
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveDTO
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseKomposittDTO
import no.nav.ung.deltakelseopplyser.utils.personIdent
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.transaction.annotation.Transactional
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
    private val oppgaveService: OppgaveService,
    private val oppgaveMapperService: OppgaveMapperService,
    private val tokenValidationContextHolder: SpringTokenValidationContextHolder,
    private val ungOppgaverService: UngOppgaverService,
    @Value("\${OPPGAVER_I_UNG_SAK_ENABLED}") private val oppgaverIUngSakEnabled: Boolean,
) {

    @GetMapping("/hent/alle", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Henter alle deltakelser for en deltaker i ungdomsprogrammet")
    @ResponseStatus(HttpStatus.OK)
    fun hentAlleMineDeltakelser(): List<DeltakelseKomposittDTO> {
        val personIdent = tokenValidationContextHolder.personIdent()
        return registerService.hentAlleDeltakelsePerioderForDeltaker(deltakerIdentEllerAktørId = personIdent)
    }

    @PutMapping("/{id}/marker-har-sokt", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Markerer at deltakelsen er søkt om")
    @ResponseStatus(HttpStatus.OK)
    @Transactional(TRANSACTION_MANAGER)
    fun markerDeltakelseSomSøkt(@PathVariable id: UUID): DeltakelseKomposittDTO {
        val alleDeltakersIdenter = deltakerService.hentDeltakterIdenter(tokenValidationContextHolder.personIdent())
        val personPåDeltakelsen = registerService.hentFraProgram(id).deltaker.deltakerIdent
        if (!alleDeltakersIdenter.contains(personPåDeltakelsen)) {
            throw ErrorResponseException(
                HttpStatus.FORBIDDEN,
                ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Bruker kan kun endre på egne data"),
                null
            )
        }

        val deltakelseDTO = registerService.markerSomHarSøkt(id)
        return DeltakelseKomposittDTO(
            deltakelse = deltakelseDTO,
            oppgaver = deltakerService.hentDeltakersOppgaver(personPåDeltakelsen)
                .map { oppgaveMapperService.mapOppgaveTilDTO(it) }
        )
    }

    @GetMapping("/oppgave/{oppgaveReferanse}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Henter en oppgave for en gitt deltakelse")
    @ResponseStatus(HttpStatus.OK)
    fun hentDeltakersOppgave(@PathVariable oppgaveReferanse: UUID): OppgaveDTO {
        val personIdent = tokenValidationContextHolder.personIdent()
        val oppgaveDAO = deltakerService.hentDeltakersOppgaver(personIdent)
            .find { it.oppgaveReferanse == oppgaveReferanse } ?: throw ErrorResponseException(
            HttpStatus.NOT_FOUND,
            ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Fant ingen oppgave med referanse $oppgaveReferanse for deltaker."
            ),
            null
        )

        return oppgaveMapperService.mapOppgaveTilDTO(oppgaveDAO)
    }

    @GetMapping("/oppgave/{oppgaveReferanse}/lukk", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Markerer en oppgave som lukket")
    @ResponseStatus(HttpStatus.OK)
    @Transactional(TRANSACTION_MANAGER)
    fun markerOppgaveSomLukket(@PathVariable oppgaveReferanse: UUID): OppgaveDTO {
        if (oppgaverIUngSakEnabled) {
            ungOppgaverService.lukkOppgave(oppgaveReferanse)
        }
        return oppgaveService.lukkOppgave(oppgaveReferanse = oppgaveReferanse)
    }

    @GetMapping("/oppgave/{oppgaveReferanse}/apnet", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Markerer en oppgave som åpnet")
    @ResponseStatus(HttpStatus.OK)
    @Transactional(TRANSACTION_MANAGER)
    fun markerOppgaveSomÅpnet(@PathVariable oppgaveReferanse: UUID): OppgaveDTO {
        if (oppgaverIUngSakEnabled) {
            ungOppgaverService.åpneOppgave(oppgaveReferanse)
        }
        return oppgaveService.åpneOppgave(oppgaveReferanse = oppgaveReferanse)
    }

    @GetMapping("/oppgave/{oppgaveReferanse}/løst", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Markerer en oppgave som åpnet")
    @ResponseStatus(HttpStatus.OK)
    @Transactional(TRANSACTION_MANAGER)
    fun markerOppgaveSomLøst(@PathVariable oppgaveReferanse: UUID): OppgaveDTO {
        return oppgaveService.løsOppgave(oppgaveReferanse = oppgaveReferanse)
    }
}
