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

        private val ENHETER_WHITE_LIST = listOf<String>(
            "sy433h",
            "ry911x",
            "ty596s",
            "ke137x",
            "ge443j",
            "gu821v",
            "ha577m",
            "me316v",
            "ra244a",
            "me959m",
            "da858y",
        )
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
    fun hentEnheterKnyttetNavIdenter(): DeltakelsePerEnhetResponse {
        tilgangskontrollService.krevDriftsTilgang(BeskyttetRessursActionAttributt.READ)

        val alleDeltakelser: List<DeltakelseDAO> = deltakelseRepository.findAll()
        logger.info("Henter enheter for {} deltakelser", alleDeltakelser.size)

        val unikeNavIdenterFraDeltakelser: Set<String> = alleDeltakelser
            .map { deltakelse -> deltakelse.opprettetAv.replace(VEILEDER_SUFFIX, "").trim() }
            .distinctBy { navIdent -> navIdent }
            .toSet()

        logger.info(
            "Fant {} unike NAV-identer fra {} deltakelser",
            unikeNavIdenterFraDeltakelser.size,
            alleDeltakelser.size
        )

        val resursserMedEnheter = nomApiService.hentResursserMedEnheter(unikeNavIdenterFraDeltakelser)
            // Filtrer på enhets-id for å kun ta med relevante enheter
            .map { it.copy(enheter = it.enheter.filter { enhet -> ENHETER_WHITE_LIST.contains(enhet.id) }) }

        // logg ressurser med flere enheter enn 1
        resursserMedEnheter.forEach {
            if (it.enheter.size > 1) {
                logger.warn("NAV-ident er knyttet til flere enn 1 enhet: ${it.enheter.map { enhet -> enhet.id + " " + enhet.navn }}"
                )
            }
        }

        // Tell antall deltakelser per enhet
        // Opprett en map fra navIdent til deltakelser
        val deltakelserPerNavIdent = alleDeltakelser
            .groupBy { deltakelse -> deltakelse.opprettetAv.replace(VEILEDER_SUFFIX, "").trim() }

        // Tell antall deltakelser per enhet
        val deltakelserPerEnhet = mutableMapOf<String, Int>()

        resursserMedEnheter.forEach { ressursMedEnheter ->
            val antallDeltakelserForNavIdent = deltakelserPerNavIdent[ressursMedEnheter.navIdent]?.size ?: 0
            // Kun tell deltakelser for den første enheten til hver person for å unngå dobbeltelling
            if (ressursMedEnheter.enheter.isNotEmpty() && antallDeltakelserForNavIdent > 0) {
                val primærenhet = ressursMedEnheter.enheter.first()
                deltakelserPerEnhet[primærenhet.navn] = (deltakelserPerEnhet[primærenhet.navn] ?: 0) + antallDeltakelserForNavIdent
            }
        }

        return DeltakelsePerEnhetResponse(
            deltakelserPerEnhet,
            alleDeltakelser.size,
            resursserMedEnheter
                .flatMap { it.enheter }
                .distinctBy { it.id }
                .toSet()
        )
    }

    data class DeltakelsePerEnhetResponse(
        val deltakelserPerEnhet: MutableMap<String, Int>,
        val antallDeltakelser: Int,
        val unikeEnheter: Set<OrgEnhet>,
    )

    data class DeltakelseDiagnostikkDto(
        val deltakelse: DeltakelseDTO,
        val historikk: List<DeltakelseHistorikk>,
    )
}
