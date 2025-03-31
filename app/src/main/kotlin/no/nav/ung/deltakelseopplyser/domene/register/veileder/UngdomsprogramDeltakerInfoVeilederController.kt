package no.nav.ung.deltakelseopplyser.domene.register.veileder

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.ung.deltakelseopplyser.config.Issuers.TOKEN_X
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.*

@Profile("!prod-gcp")
@RestController
@RequestMapping("/oppslag")
@RequiredIssuers(
    ProtectedWithClaims(
        issuer = TOKEN_X,
        claimMap = ["acr=Level4", "acr=idporten-loa-high"],
        combineWithOr = true
    )
)
@Tag(name = "Oppslag", description = "API for å hente informasjon om deltakere.")
class UngdomsprogramDeltakerInfoVeilederController(
    private val deltakerService: DeltakerService,
) {

    @PostMapping("/deltaker", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Hent personlia for en deltaker")
    @ResponseStatus(HttpStatus.OK)
    fun hentDeltakerInfoGittDeltaker(@RequestBody deltakerDTO: DeltakerDTO): DeltakerService.DeltakerPersonlia? {
        // TODO: Må implementere tilgangskontroll for å sjekke at veileder har tilgang til å hente personlia for deltakeren
        return deltakerService.hentDeltakerInfo(deltakerIdent = deltakerDTO.deltakerIdent)
    }

    @GetMapping("/deltaker/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Hent personlia for en deltaker gitt en UUID")
    @ResponseStatus(HttpStatus.OK)
    fun hentDeltakerInfoGittDeltakerId(@PathVariable id: UUID): DeltakerService.DeltakerPersonlia? {
        // TODO: Må implementere tilgangskontroll for å sjekke at veileder har tilgang til å hente personlia for deltakeren
        return deltakerService.hentDeltakerInfo(deltakerId = id)
    }
}
