package no.nav.ung.deltakelseopplyser.diagnostikk

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.k9.felles.log.audit.EventClassId
import no.nav.nom.generated.hentressurser.OrgEnhet
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.sif.abac.kontrakt.abac.AksjonspunktType
import no.nav.sif.abac.kontrakt.abac.BeskyttetRessursActionAttributt
import no.nav.sif.abac.kontrakt.abac.ResourceType
import no.nav.sif.abac.kontrakt.abac.dto.OperasjonDto
import no.nav.sif.abac.kontrakt.abac.dto.PersonerOperasjonDto
import no.nav.sif.abac.kontrakt.person.PersonIdent
import no.nav.ung.deltakelseopplyser.audit.SporingsloggService
import no.nav.ung.deltakelseopplyser.config.Issuers
import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseDAO
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramDeltakelseRepository
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService.Companion.mapToDTO
import no.nav.ung.deltakelseopplyser.domene.register.historikk.DeltakelseHistorikk
import no.nav.ung.deltakelseopplyser.domene.register.historikk.DeltakelseHistorikkService
import no.nav.ung.deltakelseopplyser.historikk.AuditorAwareImpl.Companion.VEILEDER_SUFFIX
import no.nav.ung.deltakelseopplyser.integration.abac.TilgangskontrollService
import no.nav.ung.deltakelseopplyser.integration.nom.api.NomApiService
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseDTO
import org.slf4j.LoggerFactory
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
    private val sporingsloggService: SporingsloggService,
    private val nomApiService: NomApiService,
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(DiagnostikkDriftController::class.java)
    }

    @PostMapping(
        "/hent/deltakelse/{deltakelseId}",
        consumes = [MediaType.TEXT_PLAIN_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(summary = "Hent deltakelse gitt id")
    @ResponseStatus(HttpStatus.OK)
    fun hentDeltakelse(
        @PathVariable deltakelseId: UUID,
        @RequestBody begrunnelse: String,
    ): DeltakelseDiagnostikkDto {
        val deltakelse: Optional<DeltakelseDAO> = deltakelseRepository.findById(deltakelseId)
        val deltakelseDTO: DeltakelseDTO =
            deltakelse.map { it.mapToDTO() }
                .orElseThrow { IllegalArgumentException("Fant ikke deltakelse: $deltakelseId") }

        val deltakerPersonIdent = PersonIdent(deltakelseDTO.deltaker.deltakerIdent)
        tilgangskontrollService.krevTilgangTilPersonerForInnloggetBruker(
            PersonerOperasjonDto(
                null,
                listOf(deltakerPersonIdent),
                OperasjonDto(ResourceType.DRIFT, BeskyttetRessursActionAttributt.READ, setOf<AksjonspunktType>())
            )
        ).also {
            sporingsloggService.logg(
                url = "/diagnostikk/hent/deltakelse/$deltakelseId",
                beskrivelse = begrunnelse,
                bruker = deltakerPersonIdent,
                eventClassId = EventClassId.AUDIT_ACCESS
            )
        }

        val deltakelseHistorikk: List<DeltakelseHistorikk> =
            deltakelseHistorikkService.deltakelseHistorikk(deltakelseId)

        return DeltakelseDiagnostikkDto(
            deltakelse = deltakelseDTO,
            historikk = deltakelseHistorikk
        )
    }

    @GetMapping("/hent/enheter-knyttet-nav-identer", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Hent enheter knyttet til alle nav-identer")
    @ResponseStatus(HttpStatus.OK)
    fun hentEnheterKnyttetNavIdenter(): List<OrgEnhet> {
        val unikeNavIdenter: Set<String> = deltakelseRepository.findAll()
            .map { it.id }
            .also { logger.info("Henter hoistorikk for {} deltakelser", it.size) }
            .flatMap { deltakelseId: UUID ->
                deltakelseHistorikkService.deltakelseHistorikk(id = deltakelseId)
                    .also { logger.info("Fant totalt {} historikkinnslag", it.size) }
                    .distinctBy { historikk: DeltakelseHistorikk -> historikk.endretAv }
                    .also { logger.info("Redusert til ${it.size} unike (endretAv) historikkinnslag") }
            }
            .also { historikk ->
                logger.info(
                    "Filtrerer ut {} historikkinnslag med endretAv=null",
                    historikk.filter { it.endretAv == null }.size)
            }
            .mapNotNull { it.endretAv }
            .filter { it.contains(VEILEDER_SUFFIX) }
            .also { logger.info("Redusert til {} historikkinnslag endret av veileder", it.size) }
            .map { it.replace(VEILEDER_SUFFIX, "").trim() }
            .toSet()

        val enheter = nomApiService.hentEnheter(unikeNavIdenter)
        logger.info("Fant totalt {} unike enheter knyttet til {} unike nav-identer", enheter.size, unikeNavIdenter.size)
        return enheter
    }


    data class DeltakelseDiagnostikkDto(
        val deltakelse: DeltakelseDTO,
        val historikk: List<DeltakelseHistorikk>,
    )
}
