package no.nav.ung.deltakelseopplyser.domene.register.deltaker

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.ung.deltakelseopplyser.config.Issuers.TOKEN_X
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/deltaker")
@RequiredIssuers(
    ProtectedWithClaims(
        issuer = TOKEN_X,
        claimMap = ["acr=Level4", "acr=idporten-loa-high"],
        combineWithOr = true
    )
)
@Tag(name = "Deltaker", description = "API for å hente registeropplysninger om deltaker i ungdomsprogrammet")
class DeltakerRegisterInfoController(
    private val deltakerService: DeltakerService,
) {

    @GetMapping("/aktiv-kontonummer", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Henter kontonummer for en deltaker i ungdomsprogrammet")
    @ResponseStatus(HttpStatus.OK)
    fun hentAlleMineDeltakelser(): DeltakerService.KontonummerDTO {
        return deltakerService.hentKontonummer()
    }
}
