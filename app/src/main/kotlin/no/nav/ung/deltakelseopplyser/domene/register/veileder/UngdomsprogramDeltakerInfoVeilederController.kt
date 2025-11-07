package no.nav.ung.deltakelseopplyser.domene.register.veileder

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.sif.abac.kontrakt.abac.BeskyttetRessursActionAttributt.READ
import no.nav.sif.abac.kontrakt.person.PersonIdent
import no.nav.ung.deltakelseopplyser.audit.EventClassId
import no.nav.ung.deltakelseopplyser.audit.SporingsloggService
import no.nav.ung.deltakelseopplyser.config.Issuers
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerPersonalia
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.integration.abac.TilgangskontrollService
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/oppslag")
@RequiredIssuers(
    ProtectedWithClaims(issuer = Issuers.AZURE)
)

@Tag(name = "Oppslag", description = "API for å hente informasjon om deltakere.")
class UngdomsprogramDeltakerInfoVeilederController(
    private val sporingsloggService: SporingsloggService,
    private val tilgangskontrollService: TilgangskontrollService,
    private val deltakerService: DeltakerService,
) {

    @PostMapping("/deltaker", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Hent personalia for en deltaker")
    @ResponseStatus(HttpStatus.OK)
    fun hentDeltakerInfoGittDeltaker(@RequestBody deltakerDTO: DeltakerDTO): DeltakerPersonalia? {
        tilgangskontrollService.krevAnsattTilgang(READ, listOf(PersonIdent.fra(deltakerDTO.deltakerIdent)))
        return deltakerService.hentDeltakerInfo(deltakerIdent = deltakerDTO.deltakerIdent)
            .also {
                sporingsloggService.logg(
                    "/deltaker",
                    "Hent personalia for en deltaker",
                    PersonIdent.fra(deltakerDTO.deltakerIdent),
                    EventClassId.AUDIT_ACCESS
                )
            }
    }

    @GetMapping("/deltaker/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Hent personlia for en deltaker gitt en UUID")
    @ResponseStatus(HttpStatus.OK)
    fun hentDeltakerInfoGittDeltakerId(@PathVariable id: UUID): DeltakerPersonalia? {
        val deltakerInfo = deltakerService.hentDeltakerInfo(deltakerId = id) ?: return null
        val personIdent = PersonIdent.fra(deltakerInfo.deltakerIdent)
        tilgangskontrollService.krevAnsattTilgang(READ, listOf(personIdent))
        return deltakerInfo
            .also {
                sporingsloggService.logg(
                    "/deltaker/{id}",
                    "Hent personalia for en deltaker gitt UUID",
                    personIdent,
                    EventClassId.AUDIT_ACCESS
                )
            }
    }
}
