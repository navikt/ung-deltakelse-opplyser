package no.nav.ung.deltakelseopplyser.register.veileder

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.ung.deltakelseopplyser.config.Issuers.TOKEN_X
import no.nav.ung.deltakelseopplyser.register.DeltakelseInnmeldingDTO
import no.nav.ung.deltakelseopplyser.register.DeltakelseOpplysningDTO
import no.nav.ung.deltakelseopplyser.register.DeltakelseUtmeldingDTO
import no.nav.ung.deltakelseopplyser.register.DeltakerDTO
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
@Tag(name = "Veileder", description = "API for å legge til, hente, oppdatere og fjerne deltakelser i ungdomsprogrammet")
class UngdomsprogramRegisterVeilederController(
    private val registerService: UngdomsprogramregisterService,
) {

    @Deprecated("Bruk /innmelding i stedet")
    @PostMapping("/legg-til", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Legg til en ny deltakelse i ungdomsprogrammet")
    @ResponseStatus(HttpStatus.CREATED)
    fun leggTilIProgram(@RequestBody deltakelseOpplysningDTO: DeltakelseOpplysningDTO): DeltakelseOpplysningDTO {
        return registerService.leggTilIProgram(deltakelseOpplysningDTO)
    }

    @PostMapping(
        "/deltakelse/innmelding",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(summary = "Meld inn en deltaker i ungdomsprogrammet.")
    fun meldInnDeltaker(@RequestBody deltakelseInnmeldingDTO: DeltakelseInnmeldingDTO): DeltakelseOpplysningDTO {

        val deltakelseOpplysningDTO = DeltakelseOpplysningDTO(
            deltakerIdent = deltakelseInnmeldingDTO.deltakerIdent,
            deltaker = DeltakerDTO(deltakerIdent = deltakelseInnmeldingDTO.deltakerIdent),
            fraOgMed = deltakelseInnmeldingDTO.startdato,
            harSøkt = false
        )

        return registerService.leggTilIProgram(deltakelseOpplysningDTO)
    }

    @PutMapping("/deltakelse/{deltakelseId}/avslutt", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Avslutter en deltakelse i ungdomsprogrammet")
    @ResponseStatus(HttpStatus.OK)
    fun meldUtDeltaker(
        @PathVariable deltakelseId: UUID,
        @RequestBody deltakelseUtmeldingDTO: DeltakelseUtmeldingDTO,
    ): DeltakelseOpplysningDTO {
        val eksisterendeDeltakelse = registerService.hentFraProgram(deltakelseId)
        val utmeldtDeltakelse = eksisterendeDeltakelse.copy(tilOgMed = deltakelseUtmeldingDTO.utmeldingsdato)
        return registerService.oppdaterProgram(deltakelseId, utmeldtDeltakelse)
    }

    @GetMapping("/deltaker/{deltakerId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Hent alle deltakelser for en deltaker i ungdomsprogrammet")
    @ResponseStatus(HttpStatus.OK)
    fun hentAlleProgramopplysningerForDeltaker(@PathVariable deltakerId: UUID): List<DeltakelseOpplysningDTO> {
        return registerService.hentAlleForDeltakerId(deltakerId)
    }

    @PutMapping("/deltakelse/{deltakelseId}/oppdater/", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Oppdater opplysninger for en eksisterende deltakelse i ungdomsprogrammet")
    @ResponseStatus(HttpStatus.OK)
    fun oppdaterFraProgram(
        @PathVariable deltakelseId: UUID,
        @RequestBody deltakelseOpplysningDTO: DeltakelseOpplysningDTO,
    ): DeltakelseOpplysningDTO {
        return registerService.oppdaterProgram(deltakelseId, deltakelseOpplysningDTO)
    }


    @DeleteMapping("/deltakelse/{deltakelseId}/fjern", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Fjern en deltakelse fra ungdomsprogrammet")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun fjernFraProgram(@PathVariable deltakelseId: UUID) {
        registerService.fjernFraProgram(deltakelseId)
    }
}
