package no.nav.ung.deltakelseopplyser.diagnostikk

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.sif.abac.kontrakt.abac.AksjonspunktType
import no.nav.sif.abac.kontrakt.abac.BeskyttetRessursActionAttributt
import no.nav.sif.abac.kontrakt.abac.ResourceType
import no.nav.sif.abac.kontrakt.abac.dto.OperasjonDto
import no.nav.sif.abac.kontrakt.abac.dto.PersonerOperasjonDto
import no.nav.sif.abac.kontrakt.person.PersonIdent
import no.nav.ung.deltakelseopplyser.config.Issuers
import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseDAO
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramDeltakelseRepository
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService.Companion.mapToDTO
import no.nav.ung.deltakelseopplyser.domene.register.historikk.DeltakelseHistorikk
import no.nav.ung.deltakelseopplyser.domene.register.historikk.DeltakelseHistorikkService
import no.nav.ung.deltakelseopplyser.integration.abac.TilgangskontrollService
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseDTO
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/diagnostikk")
@RequiredIssuers(
    ProtectedWithClaims(issuer = Issuers.AZURE)
)
@Tag(
    name = "Henter data for diagnostikk og feilretting",
    description = "API for å hente informasjon brukt for feilretting. Er sikret med Azure."
)
class DiagnostikkDriftController(
    private val deltakelseRepository: UngdomsprogramDeltakelseRepository,
    private val tilgangskontrollService: TilgangskontrollService,
    private val deltakelseHistorikkService: DeltakelseHistorikkService,
) {

    @GetMapping("/hent/deltakelse", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Hent deltakelse gitt id")
    @ResponseStatus(HttpStatus.OK)
    fun hentDeltakelse(@PathVariable deltakelseId: UUID): DeltakelseDiagnostikkDto {
        val deltakelse: Optional<DeltakelseDAO> = deltakelseRepository.findById(deltakelseId)
        val deltakelseDTO: DeltakelseDTO =
            deltakelse.map { it.mapToDTO() }.orElseThrow { IllegalArgumentException("Fant ikke deltakelse: $deltakelseId") }

        tilgangskontrollService.krevTilgangTilPersonerForInnloggetBruker(PersonerOperasjonDto(
            null,
            listOf(PersonIdent(deltakelseDTO.deltaker.deltakerIdent)),
            OperasjonDto(ResourceType.DRIFT, BeskyttetRessursActionAttributt.READ, setOf<AksjonspunktType>())
        ))

        val deltakelseHistorikk: List<DeltakelseHistorikk> = deltakelseHistorikkService.deltakelseHistorikk(deltakelseId)

        return DeltakelseDiagnostikkDto(
            deltakelse = deltakelseDTO,
            historikk = deltakelseHistorikk
        )
    }

    data class DeltakelseDiagnostikkDto(
        val deltakelse: DeltakelseDTO,
        val historikk: List<DeltakelseHistorikk>,
    )
}
