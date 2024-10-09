package no.nav.ung.deltakelseopplyser.register.veileder

import io.swagger.v3.oas.annotations.Operation
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.ung.deltakelseopplyser.config.Issuers.TOKEN_X
import no.nav.ung.deltakelseopplyser.register.DeltakerOpplysningDTO
import no.nav.ung.deltakelseopplyser.register.DeltakerProgramOpplysningDTO
import no.nav.ung.deltakelseopplyser.register.UngdomsprogramregisterService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.*


@RestController
@RequestMapping("/veileder/register")
@RequiredIssuers(
    ProtectedWithClaims(
        issuer = TOKEN_X,
        claimMap = ["acr=Level4", "acr=idporten-loa-high"],
        combineWithOr = true
    )
)
class UngdomsprogramRegisterVeilederController(
    private val registerService: UngdomsprogramregisterService,
) {

    /**
     * Legger til en deltaker i ungdomsprogrammet.
     */
    @PostMapping("/legg-til", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Legg til en deltaker i ungdomsprogrammet")
    @ResponseStatus(HttpStatus.CREATED)
    fun leggTilIProgram(@RequestBody deltakerProgramOpplysningDTO: DeltakerProgramOpplysningDTO): DeltakerProgramOpplysningDTO {
        return registerService.leggTilIProgram(deltakerProgramOpplysningDTO)
    }

    /**
     * Henter opplysninger for en deltaker i ungdomsprogrammet.
     */
    @GetMapping("/hent/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Hent opplysninger for en deltaker i ungdomsprogrammet")
    @ResponseStatus(HttpStatus.OK)
    fun hentOpplysningForDeltaker(@PathVariable id: UUID): DeltakerProgramOpplysningDTO {
        return registerService.hentFraProgram(id)
    }

    /**
     * Henter alle opplysninger for en deltaker i ungdomsprogrammet.
     */
    @GetMapping("/hent/alle", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Hent alle opplysninger for en deltaker i ungdomsprogrammet")
    @ResponseStatus(HttpStatus.OK)
    fun hentAlleProgramopplysningerForDeltaker(@RequestBody deltakerOpplysningDTO: DeltakerOpplysningDTO): List<DeltakerProgramOpplysningDTO> {
        return registerService.hentAlleForDeltaker(deltakerIdent = deltakerOpplysningDTO.deltakerIdent)
    }

    /**
     * Oppdaterer opplysninger for en deltaker i ungdomsprogrammet.
     */
    @PutMapping("/oppdater/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Oppdater opplysninger for en deltaker i ungdomsprogrammet")
    @ResponseStatus(HttpStatus.OK)
    fun oppdaterFraProgram(
        @PathVariable id: UUID,
        @RequestBody deltakerProgramOpplysningDTO: DeltakerProgramOpplysningDTO,
    ): DeltakerProgramOpplysningDTO {
        return registerService.oppdaterProgram(id, deltakerProgramOpplysningDTO)
    }

    /**
     * Fjerner en deltaker fra ungdomsprogrammet.
     */
    @DeleteMapping("/fjern/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Fjern en deltaker fra ungdomsprogrammet")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun fjernFraProgram(@PathVariable id: UUID) {
        registerService.fjernFraProgram(id)
    }
}
