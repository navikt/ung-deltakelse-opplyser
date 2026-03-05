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
import no.nav.ung.brukerdialog.kontrakt.oppgaver.MigreringsResultat
import no.nav.ung.deltakelseopplyser.audit.SporingsloggService
import no.nav.ung.deltakelseopplyser.config.Issuers
import no.nav.ung.deltakelseopplyser.config.TxConfiguration.Companion.TRANSACTION_MANAGER
import no.nav.ung.deltakelseopplyser.domene.oppgave.OppgaveMapperService
import no.nav.ung.deltakelseopplyser.domene.oppgave.OppgaveRepository
import no.nav.ung.deltakelseopplyser.domene.oppgave.OppgaveService
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService.Companion.mapToDTO
import no.nav.ung.deltakelseopplyser.integration.abac.TilgangskontrollService
import no.nav.ung.deltakelseopplyser.integration.pdl.api.PdlService
import no.nav.ung.deltakelseopplyser.integration.ungsak.UngBrukerdialogService
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveDTO
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/forvaltning/oppgave")
@RequiredIssuers(
    ProtectedWithClaims(issuer = Issuers.AZURE)
)
@Tag(
    name = "Forvaltningsendepunkter for feilretting",
    description = "API for å endre data på oppgaver i forbindelse med forvaltning. Er sikret med Azure."
)
class OppgaveForvaltningController(
    private val oppgaveRepository: OppgaveRepository,
    private val oppgaveService: OppgaveService,
    private val oppgaveMapperService: OppgaveMapperService,
    private val pdlService: PdlService,
    private val ungBrukerdialogService: UngBrukerdialogService,
    private val tilgangskontrollService: TilgangskontrollService,
    private val sporingsloggService: SporingsloggService,
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(OppgaveForvaltningController::class.java)
    }

    @Transactional(TRANSACTION_MANAGER)
    @PostMapping(
        "/avbryt/{oppgaveReferanse}",
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(summary = "Avbryt oppgave gitt oppgavereferanse")
    @ResponseStatus(HttpStatus.OK)
    fun avbrytOppgave(
        @PathVariable oppgaveReferanse: UUID,
        @RequestParam begrunnelse: String,
    ): OppgaveDTO {
        val oppgave = oppgaveRepository.findByOppgaveReferanse(oppgaveReferanse)
            ?: throw IllegalArgumentException("Fant ikke oppgave med referanse: $oppgaveReferanse")

        val deltakelseDto = oppgave.deltaker.deltakelseList.first().mapToDTO()
        val deltakerPersonIdent = PersonIdent(deltakelseDto.deltaker.deltakerIdent)

        tilgangskontrollService.krevTilgangTilPersonerForInnloggetBruker(
            PersonerOperasjonDto(
                null,
                listOf(deltakerPersonIdent),
                OperasjonDto(ResourceType.DRIFT, BeskyttetRessursActionAttributt.UPDATE, setOf<AksjonspunktType>())
            )
        ).also {
            sporingsloggService.logg(
                url = "/forvaltning/oppgave/avbryt/$oppgaveReferanse",
                beskrivelse = begrunnelse,
                bruker = deltakerPersonIdent,
                eventClassId = EventClassId.AUDIT_ACCESS
            )
        }
        return oppgaveService.avbrytOppgave(oppgave.deltaker, oppgaveReferanse)
    }

    @Transactional(TRANSACTION_MANAGER)
    @PostMapping(
        "/migrer",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(
        summary = "Migrer alle oppgaver til ung-brukerdialog",
        description = "Henter alle oppgaver fra databasen og sender dem til ung-brukerdialog for migrering. Idempotent - oppgaver som allerede finnes hoppes over."
    )
    @ResponseStatus(HttpStatus.OK)
    fun migrerOppgaver(@RequestParam maksAntall: Int = 100): MigreringsResultat {
        tilgangskontrollService.krevDriftsTilgang(BeskyttetRessursActionAttributt.READ)

        val alleOppgaver = oppgaveRepository.findAllByErMigrertFalse(PageRequest.of(0, maksAntall))
        logger.info("Starter migrering av ${alleOppgaver.size} ikke-migrerte oppgaver til ung-brukerdialog (maks: $maksAntall)")

        var totaltOpprettet = 0
        var totaltHoppetOver = 0

        alleOppgaver
            .groupBy { it.deltaker.deltakerIdent }
            .forEach { (deltakerIdent, oppgaverForDeltaker) ->
                val aktørId = pdlService.hentAktørIder(deltakerIdent)
                    .firstOrNull { !it.historisk }
                    ?.ident

                if (aktørId == null) {
                    logger.warn("Fant ingen aktørId for deltaker - hopper over ${oppgaverForDeltaker.size} oppgave(r)")
                    totaltHoppetOver += oppgaverForDeltaker.size
                    return@forEach
                }

                val oppgaveDTOer: List<OppgaveDTO> = oppgaverForDeltaker.map {
                    oppgaveMapperService.mapOppgaveTilDTO(it)
                }

                val resultat = ungBrukerdialogService.migrerOppgaver(aktørId, oppgaveDTOer)
                logger.info("Migrert ${oppgaveDTOer.size} oppgave(r) for deltaker: ${resultat.antallOpprettet} opprettet, ${resultat.antallHoppetOver} hoppet over")

                if (resultat.antallOpprettet > 0) {
                    oppgaverForDeltaker.forEach { it.markerSomMigrert() }
                    oppgaveRepository.saveAll(oppgaverForDeltaker)
                }

                totaltOpprettet += resultat.antallOpprettet
                totaltHoppetOver += resultat.antallHoppetOver
            }

        val totalResultat = MigreringsResultat(totaltOpprettet, totaltHoppetOver)
        logger.info("Migrering fullført: ${totalResultat.antallOpprettet} opprettet, ${totalResultat.antallHoppetOver} hoppet over, ${totalResultat.antallTotalt} totalt")
        return totalResultat
    }
}
