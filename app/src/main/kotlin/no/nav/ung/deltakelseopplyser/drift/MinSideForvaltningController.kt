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
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.minside.mikrofrontend.MicrofrontendService
import no.nav.ung.deltakelseopplyser.domene.minside.mikrofrontend.MicrofrontendStatus
import no.nav.ung.deltakelseopplyser.domene.minside.mikrofrontend.MinSideMicrofrontendStatusDAO
import no.nav.ung.deltakelseopplyser.integration.abac.TilgangskontrollService
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveDTO
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

@RestController
@RequestMapping("/forvaltning")
@RequiredIssuers(
    ProtectedWithClaims(issuer = Issuers.AZURE)
)
@Tag(
    name = "Forvaltningsendepunkter for feilretting",
    description = "API for å endre data på oppgaver i forbindelse med forvaltning. Er sikret med Azure."
)
class MinSideForvaltningController(
    private val deltakerService: DeltakerService,
    private val microfrontendService: MicrofrontendService,
    private val tilgangskontrollService: TilgangskontrollService,
    private val sporingsloggService: SporingsloggService,
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(MinSideForvaltningController::class.java)
    }

    @PostMapping(
        "/min-side/innsyn/aktiver/{deltakerId}",
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(summary = "Aktiver innsyn for deltaker")
    @ResponseStatus(HttpStatus.OK)
    fun aktiverInnsyn(
        @PathVariable deltakerId: UUID,
        @RequestParam begrunnelse: String,
    ) {
        val deltaker =
            deltakerService.finnDeltakerGittId(deltakerId).orElseThrow { IllegalStateException("Fant ikke deltaker") }

        val deltakerPersonIdent = PersonIdent(deltaker.deltakerIdent)
        tilgangskontrollService.krevTilgangTilPersonerForInnloggetBruker(
            PersonerOperasjonDto(
                null,
                listOf(deltakerPersonIdent),
                OperasjonDto(ResourceType.DRIFT, BeskyttetRessursActionAttributt.UPDATE, setOf<AksjonspunktType>())
            )
        ).also {
            sporingsloggService.logg(
                url = "/min-side/innsyn/aktiver/$deltakerId",
                beskrivelse = begrunnelse,
                bruker = deltakerPersonIdent,
                eventClassId = EventClassId.AUDIT_ACCESS
            )
        }

        deltaker.minSideMicrofrontendStatusDAO
            ?.also {  logger.info("Deaktiverer eksisterende mikrofrontend for deltaker med id: {}", deltaker.id) }
            ?.let { microfrontendService.deaktiverOgSlett(it) }


        logger.info("Aktiverer mikrofrontend for deltaker med id: {}", deltaker.id)
        microfrontendService.sendOgLagre(
            MinSideMicrofrontendStatusDAO(
                id = UUID.randomUUID(),
                deltaker = deltaker,
                status = MicrofrontendStatus.ENABLE,
                opprettet = ZonedDateTime.now(ZoneOffset.UTC),
            )
        ).also {
            logger.info("Mikrofrontend aktivert for deltaker med id: {}", deltaker.id)
        }

    }

}
