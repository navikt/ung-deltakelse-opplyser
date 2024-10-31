package no.nav.ung.deltakelseopplyser.register.deltaker

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.ung.deltakelseopplyser.config.Issuers.TOKEN_X
import no.nav.ung.deltakelseopplyser.register.DeltakelseOpplysningDTO
import no.nav.ung.deltakelseopplyser.register.InntektIPeriodeDTO
import no.nav.ung.deltakelseopplyser.register.UngdomsprogramDeltakelseDAO
import no.nav.ung.deltakelseopplyser.register.UngdomsprogramregisterService
import no.nav.ung.deltakelseopplyser.utils.personIdent
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/deltakelse/register")
@RequiredIssuers(
    ProtectedWithClaims(
        issuer = TOKEN_X,
        claimMap = ["acr=Level4", "acr=idporten-loa-high"],
        combineWithOr = true
    )
)
@Tag(name = "Deltakelse", description = "API for å hente opplysninger om deltakelse i ungdomsprogrammet")
class UngdomsprogramRegisterDeltakerController(
    private val registerService: UngdomsprogramregisterService,
    private val tokenValidationContextHolder: SpringTokenValidationContextHolder,
) {

    @GetMapping("/hent/alle", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Henter alle deltakelser for en deltaker i ungdomsprogrammet")
    @ResponseStatus(HttpStatus.OK)
    fun hentAlleProgramopplysningerForDeltaker(): List<DeltakelseOpplysningDTO> {
        val personIdent = tokenValidationContextHolder.personIdent()
        return registerService.hentAlleForDeltaker(deltakerIdentEllerAktørId = personIdent)
    }

    @GetMapping("/hent/alle/perioder", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Henter alle deltakelser for en deltaker i ungdomsprogrammet med rapporteringsperioder")
    @ResponseStatus(HttpStatus.OK)
    fun hentAlleProgramperiodeinfoForDeltaker(): List<DeltakelsePeriodInfo> {
        val personIdent = tokenValidationContextHolder.personIdent()
        return registerService.hentAlleDeltakelsePerioderForDeltaker(deltakerIdentEllerAktørId = personIdent)
    }

    @PutMapping("/{id}/marker-har-sokt", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Markerer at deltakelsen er søkt om")
    @ResponseStatus(HttpStatus.OK)
    fun markerDeltakelseSomSøkt(@PathVariable id: UUID): UngdomsprogramDeltakelseDAO {
        return registerService.markerSomHarSøkt(id)
    }

    @PutMapping("/{id}/registrer-inntekt-i-periode", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Registrer inntekt i en periode")
    @ResponseStatus(HttpStatus.OK)
    fun registrerInntektIPeriode(
        @PathVariable id: UUID,
        @RequestBody inntektIPeriodeDTO: InntektIPeriodeDTO,
    ): InntektIPeriodeDTO {
        return registerService.registrerInntektIPeriode(id, inntektIPeriodeDTO)
    }

    data class DeltakelsePeriodInfo(
        val deltakerIdent: String? = null,
        val programPeriodeFraOgMed: LocalDate,
        val programperiodeTilOgMed: LocalDate? = null,
        val rapporteringsPerioder: List<RapportPeriodeinfoDTO> = emptyList()
    )

    data class RapportPeriodeinfoDTO(
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate,
        val harSøkt: Boolean,
        val inntekt: Double? = null,
    )

}
