package no.nav.ung.deltakelseopplyser.register

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.ung.deltakelseopplyser.config.Issuers.TOKEN_X
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.*


@RestController("/ungdomsprogramregister")
@RequiredIssuers(
    ProtectedWithClaims(
        issuer = TOKEN_X,
        claimMap = ["acr=Level4", "acr=idporten-loa-high"],
        combineWithOr = true
    )
)
class UngdomsprogramRegisterController(
    private val registerService: UngprogramregisterService,
) {

    /**
     * Legger til en deltaker i ungdomsprogrammet.
     */
    @PostMapping("/legg-til", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(HttpStatus.OK)
    fun leggTilIProgram(@RequestBody deltakerProgramOpplysningDTO: DeltakerProgramOpplysningDTO): DeltakerProgramOpplysningDTO {
        return registerService.leggTilIProgram(deltakerProgramOpplysningDTO)
    }

    /**
     * Henter opplysninger for en deltaker i ungdomsprogrammet.
     */
    @GetMapping("/hent/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(HttpStatus.OK)
    fun hentOpplysningForDeltaker(@PathVariable id: UUID): DeltakerProgramOpplysningDTO {
        return registerService.hentFraProgram(id)
    }

    /**
     * Henter alle opplysninger for en deltaker i ungdomsprogrammet.
     */
    @PostMapping("/hent/alle", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(HttpStatus.OK)
    fun hentAlleProgramopplysningerForDeltaker(@RequestBody deltakerOpplysningDTO: DeltakerOpplysningDTO): DeltakerProgramOpplysningDTO {
        return registerService.hentAlleForDeltaker(deltakerOpplysningDTO.deltakerIdent)
    }

    /**
     * Oppdaterer opplysninger for en deltaker i ungdomsprogrammet.
     */
    @PutMapping("/oppdater/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
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
    @ResponseStatus(HttpStatus.OK)
    fun fjernFraProgram(@PathVariable id: UUID): DeltakerProgramOpplysningDTO {
        return registerService.fjernFraProgram(id)
    }
}
