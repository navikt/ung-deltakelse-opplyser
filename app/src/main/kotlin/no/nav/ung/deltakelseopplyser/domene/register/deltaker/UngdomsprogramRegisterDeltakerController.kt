package no.nav.ung.deltakelseopplyser.domene.register.deltaker

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.ung.deltakelseopplyser.config.Issuers.TOKEN_X
import no.nav.ung.deltakelseopplyser.config.TxConfiguration.Companion.TRANSACTION_MANAGER
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDAO
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveDAO.Companion.tilDTO
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService
import no.nav.ung.deltakelseopplyser.domene.minside.MineSiderService
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakelsePeriodInfo
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveStatus
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseOpplysningDTO
import no.nav.ung.deltakelseopplyser.utils.personIdent
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
    private val mineSiderService: MineSiderService,
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

    @GetMapping("/oppgave/{oppgaveReferanse}/lukk", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Markerer en oppgave som lukket")
    @ResponseStatus(HttpStatus.OK)
    fun markerOppgaveSomLukket(@PathVariable oppgaveReferanse: UUID): OppgaveDTO {
        val OPPGAVER_SOM_STØTTER_Å_LUKKES = listOf(Oppgavetype.RAPPORTER_INNTEKT)

        val (deltaker, oppgave) = hentDeltakerOppgave(oppgaveReferanse)

        if(!OPPGAVER_SOM_STØTTER_Å_LUKKES.contains(oppgave.oppgavetype)) {
            throw ErrorResponseException(
                HttpStatus.BAD_REQUEST,
                ProblemDetail.forStatusAndDetail(
                    HttpStatus.BAD_REQUEST,
                    "Oppgave med referanse $oppgaveReferanse kan kun lukkes dersom den er av type ${OPPGAVER_SOM_STØTTER_Å_LUKKES.joinToString(",")}"
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

        return oppdatertOppgave.tilDTO()
    }

    @GetMapping("/oppgave/{oppgaveReferanse}/åpnet", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Markerer en oppgave som åpnet")
    @ResponseStatus(HttpStatus.OK)
    @Transactional(TRANSACTION_MANAGER)
    fun markerOppgaveSomÅpnet(@PathVariable oppgaveReferanse: UUID): OppgaveDTO {
        val (deltaker, oppgave) = hentDeltakerOppgave(oppgaveReferanse)

        val oppdatertOppgave = oppgave.markerSomÅpnet()
        deltakerService.oppdaterDeltaker(deltaker)

        mineSiderService.deaktiverOppgave(oppgaveReferanse.toString())

        return oppdatertOppgave.tilDTO()
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
