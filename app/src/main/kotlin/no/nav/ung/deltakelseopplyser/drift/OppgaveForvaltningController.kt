package no.nav.ung.deltakelseopplyser.drift

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.k9.felles.log.audit.EventClassId
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.sif.abac.kontrakt.abac.AksjonspunktType
import no.nav.sif.abac.kontrakt.abac.BeskyttetRessursActionAttributt
import no.nav.sif.abac.kontrakt.abac.ResourceType
import no.nav.sif.abac.kontrakt.abac.dto.OperasjonDto
import no.nav.sif.abac.kontrakt.abac.dto.PersonerOperasjonDto
import no.nav.sif.abac.kontrakt.person.PersonIdent
import no.nav.ung.deltakelseopplyser.audit.SporingsloggService
import no.nav.ung.deltakelseopplyser.config.Issuers
import no.nav.ung.deltakelseopplyser.domene.oppgave.OppgaveRepository
import no.nav.ung.deltakelseopplyser.domene.oppgave.OppgaveService
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService.Companion.mapToDTO
import no.nav.ung.deltakelseopplyser.integration.abac.TilgangskontrollService
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveDTO
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/forvaltning")
@RequiredIssuers(
    ProtectedWithClaims(issuer = Issuers.AZURE)
)
@Tag(
    name = "Forvaltningsendepunkter for feilretting",
    description = "API for å endre data på oppgaver i forbindelse med forvaltning. Er sikret med Azure."
)
class OppgaveForvaltningController(
    private val oppgaveRepository: OppgaveRepository,
    private val oppgaveService: OppgaveService,
    private val tilgangskontrollService: TilgangskontrollService,
    private val sporingsloggService: SporingsloggService,
) {

    @PostMapping(
        "/oppgave/avbryt/{oppgaveReferanse}",
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(summary = "Avbryt oppgave gitt oppgavereferanse")
    @ResponseStatus(HttpStatus.OK)
    fun avbrytOppgave(
        @PathVariable oppgaveReferanse: UUID,
        @RequestParam begrunnelse: String,
    ) : OppgaveDTO {
        val oppgave = oppgaveRepository.findByOppgaveReferanse(oppgaveReferanse)
            ?: throw IllegalArgumentException("Fant ikke oppgave med referanse: $oppgaveReferanse")

        val deltakelseDto = oppgave.deltaker.deltakelseList.first().mapToDTO()
        val deltakerPersonIdent = PersonIdent(deltakelseDto.deltaker.deltakerIdent)

        tilgangskontrollService.krevTilgangTilPersonerForInnloggetBruker(
            PersonerOperasjonDto(
                null,
                listOf(deltakerPersonIdent),
                OperasjonDto(ResourceType.DRIFT, BeskyttetRessursActionAttributt.UPDATE, setOf<AksjonspunktType>())
            )
        ).also {
            sporingsloggService.logg(
                url = "/forvaltning/oppgave/avbryt/$oppgaveReferanse",
                beskrivelse = begrunnelse,
                bruker = deltakerPersonIdent,
                eventClassId = EventClassId.AUDIT_ACCESS
            )
        }
        return oppgaveService.avbrytOppgave(oppgave.deltaker, oppgaveReferanse)
    }

}
