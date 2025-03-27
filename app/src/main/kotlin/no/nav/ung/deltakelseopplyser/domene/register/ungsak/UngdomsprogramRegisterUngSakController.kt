package no.nav.ung.deltakelseopplyser.domene.register.ungsak

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.ung.deltakelseopplyser.config.Issuers
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakerOpplysningerDTO
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService
import no.nav.ung.sak.kontrakt.person.AktørIdDto
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/register")
@RequiredIssuers(
    ProtectedWithClaims(issuer = Issuers.AZURE)
)
@Tag(
    name = "Les register data",
    description = "API for å hente deltakelser for en gitt deltaker i ungdomsprogrammet. Er sikret med Azure."
)
class UngdomsprogramRegisterUngSakController(
    private val registerService: UngdomsprogramregisterService,
) {

    @PostMapping("/hent/alle", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Hent alle deltakelser for en deltaker i ungdomsprogrammet")
    @ResponseStatus(HttpStatus.OK)
    fun hentAlleDeltakelserGittDeltakerAktør(@RequestBody aktørIdDto: AktørIdDto): DeltakerOpplysningerDTO {
        val opplysninger = registerService.hentAlleForDeltaker(deltakerIdentEllerAktørId = aktørIdDto.aktorId)
        return DeltakerOpplysningerDTO(opplysninger)
    }

}
