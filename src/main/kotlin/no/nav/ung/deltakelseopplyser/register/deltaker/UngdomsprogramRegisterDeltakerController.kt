package no.nav.ung.deltakelseopplyser.register.deltaker

import io.swagger.v3.oas.annotations.Operation
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.ung.deltakelseopplyser.config.Issuers.TOKEN_X
import no.nav.ung.deltakelseopplyser.register.DeltakerProgramOpplysningDTO
import no.nav.ung.deltakelseopplyser.register.UngdomsprogramregisterService
import no.nav.ung.deltakelseopplyser.utils.personIdent
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.web.ErrorResponseException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.*


@RestController
@RequestMapping("/deltaker/register")
@RequiredIssuers(
    ProtectedWithClaims(
        issuer = TOKEN_X,
        claimMap = ["acr=Level4", "acr=idporten-loa-high"],
        combineWithOr = true
    )
)
class UngdomsprogramRegisterDeltakerController(
    private val registerService: UngdomsprogramregisterService,
    private val tokenValidationContextHolder: SpringTokenValidationContextHolder,
) {

    /**
     * Henter opplysninger for en deltaker i ungdomsprogrammet.
     */
    @GetMapping("/hent/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Hent opplysninger for en deltaker i ungdomsprogrammet")
    @ResponseStatus(HttpStatus.OK)
    fun hentOpplysningForDeltaker(@PathVariable id: UUID): DeltakerProgramOpplysningDTO {
        val programOpplysningDTO = registerService.hentFraProgram(id)
        if (programOpplysningDTO.deltakerIdent != tokenValidationContextHolder.personIdent()) {
            throw ErrorResponseException(
                HttpStatus.FORBIDDEN,
                ProblemDetail.forStatusAndDetail(
                    HttpStatus.FORBIDDEN,
                    "Programopplysningene tilhører ikke deltaker. Tilgang nektet."
                ),
                null
            )
        }
        return programOpplysningDTO
    }

    /**
     * Henter alle opplysninger for en deltaker i ungdomsprogrammet.
     */
    @GetMapping("/hent/alle", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Hent alle opplysninger for en deltaker i ungdomsprogrammet")
    @ResponseStatus(HttpStatus.OK)
    fun hentAlleProgramopplysningerForDeltaker(): List<DeltakerProgramOpplysningDTO> {
        val personIdent = tokenValidationContextHolder.personIdent()
        return registerService.hentAlleForDeltaker(deltakerIdent = personIdent)
    }
}
