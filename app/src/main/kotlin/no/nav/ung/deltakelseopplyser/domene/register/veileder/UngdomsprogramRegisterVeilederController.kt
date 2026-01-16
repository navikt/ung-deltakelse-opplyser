package no.nav.ung.deltakelseopplyser.domene.register.veileder

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.k9.felles.log.audit.EventClassId
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.sif.abac.kontrakt.abac.BeskyttetRessursActionAttributt.CREATE
import no.nav.sif.abac.kontrakt.abac.BeskyttetRessursActionAttributt.READ
import no.nav.sif.abac.kontrakt.abac.BeskyttetRessursActionAttributt.UPDATE
import no.nav.sif.abac.kontrakt.person.PersonIdent
import no.nav.ung.deltakelseopplyser.audit.SporingsloggService
import no.nav.ung.deltakelseopplyser.config.Issuers
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDAO
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService
import no.nav.ung.deltakelseopplyser.domene.register.historikk.DeltakelseHistorikkService
import no.nav.ung.deltakelseopplyser.integration.abac.TilgangskontrollService
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseDTO
import no.nav.ung.deltakelseopplyser.kontrakt.register.historikk.DeltakelseHistorikkDTO
import no.nav.ung.deltakelseopplyser.kontrakt.veileder.DeltakelseInnmeldingDTO
import no.nav.ung.deltakelseopplyser.kontrakt.veileder.DeltakelseUtmeldingDTO
import no.nav.ung.deltakelseopplyser.kontrakt.veileder.EndrePeriodeDatoDTO
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
    ProtectedWithClaims(issuer = Issuers.AZURE)
)
@Tag(name = "Veileder", description = "API for å legge til, hente, oppdatere og fjerne deltakelser i ungdomsprogrammet")
class UngdomsprogramRegisterVeilederController(
    private val sporingsloggService: SporingsloggService,
    private val tilgangskontrollService: TilgangskontrollService,
    private val registerService: UngdomsprogramregisterService,
    private val deltakelseHistorikkService: DeltakelseHistorikkService,
    private val deltakerService: DeltakerService,
) {

    @PostMapping(
        "/deltaker/innmelding",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(summary = "Meld inn en deltaker i ungdomsprogrammet.")
    fun meldInnDeltaker(@RequestBody deltakelseInnmeldingDTO: DeltakelseInnmeldingDTO): DeltakelseDTO {
        tilgangskontrollService.krevAnsattTilgang(
            CREATE,
            listOf(PersonIdent.fra(deltakelseInnmeldingDTO.deltakerIdent))
        )
        val deltakelseDTO = DeltakelseDTO(
            deltaker = DeltakerDTO(deltakerIdent = deltakelseInnmeldingDTO.deltakerIdent),
            fraOgMed = deltakelseInnmeldingDTO.startdato
        )

        return registerService.leggTilIProgram(deltakelseDTO).also {
            sporingsloggService.logg(
                "/deltaker/innmelding",
                "Meldte inn deltaker med id ${deltakelseInnmeldingDTO.deltakerIdent}",
                PersonIdent.fra(deltakelseInnmeldingDTO.deltakerIdent),
                EventClassId.AUDIT_CREATE
            )
        }
    }

    @PutMapping(
        "/deltakelse/{deltakelseId}/avslutt",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(summary = "Avslutter en deltakelse i ungdomsprogrammet")
    @ResponseStatus(HttpStatus.OK)
    fun meldUtDeltaker(
        @PathVariable deltakelseId: UUID,
        @RequestBody deltakelseUtmeldingDTO: DeltakelseUtmeldingDTO,
    ): DeltakelseDTO {
        val eksisterendeDeltakelse = registerService.hentFraProgram(deltakelseId)
        tilgangskontrollService.krevAnsattTilgang(
            UPDATE,
            listOf(PersonIdent.fra(eksisterendeDeltakelse.deltaker.deltakerIdent))
        )
        val utmeldtDeltakelse = eksisterendeDeltakelse.copy(tilOgMed = deltakelseUtmeldingDTO.utmeldingsdato)
        return registerService.avsluttDeltakelse(deltakelseId, utmeldtDeltakelse).also {
            sporingsloggService.logg(
                "/deltakelse/{deltakelseId}/avslutt",
                "Avsluttet deltakelse med id $deltakelseId",
                PersonIdent.fra(eksisterendeDeltakelse.deltaker.deltakerIdent),
                EventClassId.AUDIT_UPDATE
            )
        }
    }

    @PutMapping(
        "/deltakelse/{deltakelseId}/endre/startdato",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(summary = "Endrer startdato på en deltakelse i ungdomsprogrammet")
    @ResponseStatus(HttpStatus.OK)
    fun endreStartdato(
        @PathVariable deltakelseId: UUID,
        @RequestBody endrePeriodeDatoDTO: EndrePeriodeDatoDTO,
    ): DeltakelseDTO {
        val eksisterendeDeltakelse = registerService.hentFraProgram(deltakelseId)
        tilgangskontrollService.krevAnsattTilgang(
            UPDATE,
            listOf(PersonIdent.fra(eksisterendeDeltakelse.deltaker.deltakerIdent))
        )
        return registerService.endreStartdato(deltakelseId, endrePeriodeDatoDTO).also {
            sporingsloggService.logg(
                "/deltakelse/{deltakelseId}/endre/startdato",
                "Endret startdato for deltakelse med id $deltakelseId",
                PersonIdent.fra(eksisterendeDeltakelse.deltaker.deltakerIdent),
                EventClassId.AUDIT_UPDATE
            )
        }
    }

    @PutMapping(
        "/deltakelse/{deltakelseId}/endre/sluttdato",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(summary = "Endrer startdato på en deltakelse i ungdomsprogrammet")
    @ResponseStatus(HttpStatus.OK)
    fun endreSluttdato(
        @PathVariable deltakelseId: UUID,
        @RequestBody endrePeriodeDatoDTO: EndrePeriodeDatoDTO,
    ): DeltakelseDTO {
        val eksisterendeDeltakelse = registerService.hentFraProgram(deltakelseId)
        tilgangskontrollService.krevAnsattTilgang(
            UPDATE,
            listOf(PersonIdent.fra(eksisterendeDeltakelse.deltaker.deltakerIdent))
        )
        return registerService.endreSluttdato(deltakelseId, endrePeriodeDatoDTO).also {
            sporingsloggService.logg(
                "/deltakelse/{deltakelseId}/endre/sluttdato",
                "Endret sluttdato for deltakelse med id $deltakelseId",
                PersonIdent.fra(eksisterendeDeltakelse.deltaker.deltakerIdent),
                EventClassId.AUDIT_UPDATE
            )
        }
    }

    @GetMapping(
        "/deltaker/{deltakerId}/deltakelser",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(summary = "Hent alle deltakelser for en deltaker i ungdomsprogrammet")
    @ResponseStatus(HttpStatus.OK)
    fun hentAlleDeltakelserGittDeltakerId(@PathVariable deltakerId: UUID): List<DeltakelseDTO> {
        val deltakelser = registerService.hentAlleForDeltakerId(deltakerId)
        val personIdenter = deltakelser.map { it.deltaker.deltakerIdent }.distinct().map { PersonIdent.fra(it) }
        tilgangskontrollService.krevAnsattTilgang(READ, personIdenter)
        return deltakelser
            .also {
                sporingsloggService.logg(
                    "/deltaker/deltakerId/deltakelser",
                    "Hent alle deltakelser for en deltaker",
                    personIdenter.first(),
                    EventClassId.AUDIT_ACCESS
                )
            }
    }

    @GetMapping(
        "/deltakelse/{deltakelseId}/historikk", produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun deltakelseHistorikk(@PathVariable deltakelseId: UUID): List<DeltakelseHistorikkDTO> {
        val eksisterendeDeltakelse = registerService.hentFraProgramInkludertSlettet(deltakelseId)
        tilgangskontrollService.krevAnsattTilgang(
            READ,
            listOf(PersonIdent.fra(eksisterendeDeltakelse.deltaker.deltakerIdent))
        )
        return deltakelseHistorikkService.deltakelseHistorikk(deltakelseId)
            .map { it.tilDTO() }
            .also {
                sporingsloggService.logg(
                    "/deltakelse/{deltakelseId}/historikk",
                    "Hent historikk for deltakelse med id $deltakelseId",
                    PersonIdent.fra(eksisterendeDeltakelse.deltaker.deltakerIdent),
                    EventClassId.AUDIT_ACCESS
                )
            }
    }

    @DeleteMapping("/deltaker/{deltakerId}/fjern")
    @Operation(summary = "Fjern en deltaker fra ungdomsprogrammet")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun fjernFraProgram(@PathVariable deltakerId: UUID) {
        deltakerService.finnDeltakerGittId(deltakerId).ifPresent { deltaker: DeltakerDAO ->
            tilgangskontrollService.krevAnsattTilgang(
                UPDATE,
                listOf(PersonIdent.fra(deltaker.deltakerIdent))
            )

            registerService.fjernFraProgram(deltaker).also {
                sporingsloggService.logg(
                    "/deltaker/{deltakerId}/fjern",
                    "Fjernet deltaker med id $deltakerId",
                    PersonIdent.fra(deltaker.deltakerIdent),
                    EventClassId.AUDIT_UPDATE
                )
            }
        }
    }
}
