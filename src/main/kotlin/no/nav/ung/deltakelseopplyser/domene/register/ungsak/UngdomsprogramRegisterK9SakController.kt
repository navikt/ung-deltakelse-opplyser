package no.nav.ung.deltakelseopplyser.domene.register.ungsak

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.ung.sak.kontrakt.person.AktørIdDto
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.ung.deltakelseopplyser.config.Issuers
import no.nav.ung.deltakelseopplyser.integration.pdl.api.PdlService
import no.nav.ung.deltakelseopplyser.domene.register.DeltakerOpplysningerDTO
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/register")
@RequiredIssuers(
    ProtectedWithClaims(issuer = Issuers.AZURE)
)
@Tag(
    name = "Les register data",
    description = "API for å hente deltakelser for en gitt deltaker i ungdomsprogrammet. Er sikret med Azure."
)
class UngdomsprogramRegisterK9SakController(
    private val registerService: UngdomsprogramregisterService,
    private val pdlService: PdlService,
) {

    @PostMapping("/hent/alle", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Hent alle deltakelser for en deltaker i ungdomsprogrammet")
    @ResponseStatus(HttpStatus.OK)
    fun hentAlleDeltakelserGittDeltakerAktør(@RequestBody aktørIdDto: AktørIdDto): DeltakerOpplysningerDTO {
        val opplysninger = registerService.hentAlleForDeltaker(deltakerIdentEllerAktørId = aktørIdDto.aktorId)
        return DeltakerOpplysningerDTO(opplysninger)
    }
}
