package no.nav.ung.deltakelseopplyser.drift

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.k9.felles.log.audit.EventClassId
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
import no.nav.ung.deltakelseopplyser.domene.oppgave.OppgaveRepository
import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseDAO
import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseRepository
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService.Companion.mapToDTO
import no.nav.ung.deltakelseopplyser.domene.register.historikk.DeltakelseHistorikk
import no.nav.ung.deltakelseopplyser.domene.register.historikk.DeltakelseHistorikkService
import no.nav.ung.deltakelseopplyser.integration.abac.TilgangskontrollService
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseDTO
import no.nav.ung.deltakelseopplyser.statistikk.deltakelse.DeltakelseStatistikkService
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
    private val oppgaveRepository: OppgaveRepository,
    private val deltakelseRepository: DeltakelseRepository,
    private val tilgangskontrollService: TilgangskontrollService,
    private val deltakelseHistorikkService: DeltakelseHistorikkService,
    private val sporingsloggService: SporingsloggService,
    private val deltakelseStatistikkService: DeltakelseStatistikkService,
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

    @PostMapping(
        "/finnDeltakelseForOppgaveReferanse/{oppgaveReferanse}",
        consumes = [MediaType.TEXT_PLAIN_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(summary = "Hent deltakelse gitt oppgavereferanse")
    @ResponseStatus(HttpStatus.OK)
    fun hentDeltakelseForOppgaveReferanse(
        @PathVariable oppgaveReferanse: UUID,
        @RequestBody begrunnelse: String,
    ): DeltakelseDiagnostikkDto {
        val oppgave = oppgaveRepository.findByOppgaveReferanse(oppgaveReferanse)
            ?: throw IllegalArgumentException("Fant ikke oppgave med referanse: $oppgaveReferanse")

        val deltakelseDto = oppgave.deltaker.deltakelseList.first().mapToDTO()
        val deltakerPersonIdent = PersonIdent(deltakelseDto.deltaker.deltakerIdent)

        tilgangskontrollService.krevTilgangTilPersonerForInnloggetBruker(
            PersonerOperasjonDto(
                null,
                listOf(deltakerPersonIdent),
                OperasjonDto(ResourceType.DRIFT, BeskyttetRessursActionAttributt.READ, setOf<AksjonspunktType>())
            )
        ).also {
            sporingsloggService.logg(
                url = "/diagnostikk/finnDeltakelseForOppgaveReferanse/$oppgaveReferanse",
                beskrivelse = begrunnelse,
                bruker = deltakerPersonIdent,
                eventClassId = EventClassId.AUDIT_ACCESS
            )
        }

        val deltakelseHistorikk: List<DeltakelseHistorikk> =
            deltakelseHistorikkService.deltakelseHistorikk(oppgave.deltaker.id)

        return DeltakelseDiagnostikkDto(
            deltakelse = deltakelseDto,
            historikk = deltakelseHistorikk
        )
    }


    @GetMapping("/hent/antall-deltakelser-per-enhet-statistikk", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Hent enheter knyttet til alle nav-identer")
    @ResponseStatus(HttpStatus.OK)
    fun antallDeltakelserPerEnhetStatistikk(): Map<String, Any> {
        tilgangskontrollService.krevDriftsTilgang(BeskyttetRessursActionAttributt.READ)

        val antallDeltakelserPerKontorStatistikkV2 =
            deltakelseStatistikkService.antallDeltakelserPerEnhetStatistikk(kastFeilVedInkonsekventTelling = false)

        return mapOf(
            "deltakelerPerEnhet" to antallDeltakelserPerKontorStatistikkV2.map {
                mapOf(
                    "antallDeltakelser" to it.antallDeltakelser,
                    "kontor" to it.kontor
                )
            },
            "diagnostikk" to antallDeltakelserPerKontorStatistikkV2.first().diagnostikk
        )
    }

    data class DeltakelseDiagnostikkDto(
        val deltakelse: DeltakelseDTO,
        val historikk: List<DeltakelseHistorikk>,
    )
}
