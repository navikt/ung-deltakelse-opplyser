package no.nav.ung.deltakelseopplyser.domene.register.ungsak

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.sif.abac.kontrakt.abac.BeskyttetRessursActionAttributt
import no.nav.sif.abac.kontrakt.abac.ResourceType
import no.nav.sif.abac.kontrakt.abac.dto.OperasjonDto
import no.nav.sif.abac.kontrakt.abac.dto.PersonerOperasjonDto
import no.nav.sif.abac.kontrakt.person.AktørId
import no.nav.sif.abac.kontrakt.person.PersonIdent
import no.nav.ung.deltakelseopplyser.config.Issuers
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseDTO
import no.nav.ung.deltakelseopplyser.kontrakt.register.ungsak.DeltakelseOpplysningerDTO
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService
import no.nav.ung.deltakelseopplyser.integration.abac.TilgangskontrollService
import no.nav.ung.sak.kontrakt.person.AktørIdDto
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.util.UUID


@RestController
@RequestMapping("/register")
@RequiredIssuers(
    ProtectedWithClaims(issuer = Issuers.AZURE)
)
@Tag(
    name = "Register data (ung-sak)",
    description = "API for ung-sak: henter og oppdaterer deltakelser i ungdomsprogrammet. Sikret med Azure."
)
class UngdomsprogramRegisterUngSakController(
    private val tilgangskontrollService: TilgangskontrollService,
    private val registerService: UngdomsprogramregisterService,
) {

    @PostMapping("/hent/alle", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Hent alle deltakelser for en deltaker i ungdomsprogrammet")
    @ResponseStatus(HttpStatus.OK)
    fun hentAlleDeltakelserGittDeltakerAktør(@RequestBody aktørIdDto: AktørIdDto): DeltakelseOpplysningerDTO {
        if (tilgangskontrollService.erSystemBruker()) {
            tilgangskontrollService.krevSystemtilgang()
        } else {
            tilgangskontrollService.krevTilgangTilPersonerForInnloggetBruker(
                PersonerOperasjonDto(
                    listOf(AktørId(aktørIdDto.aktorId)),
                    listOf(),
                    OperasjonDto(ResourceType.FAGSAK, BeskyttetRessursActionAttributt.READ, setOf())
                )
            )
        }
        val opplysninger = registerService.hentIkkeSlettetForDeltaker(deltakerIdentEllerAktørId = aktørIdDto.aktorId)
        return DeltakelseOpplysningerDTO(opplysninger)
    }

    @PatchMapping("/{id}/marker-sokt", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Marker en deltakelse som søkt. Brukes av ung-sak ved journalføring av papirsøknad.")
    @ResponseStatus(HttpStatus.OK)
    fun markerDeltakelseSomSøkt(@PathVariable id: UUID, @RequestBody aktørIdDto: AktørIdDto): DeltakelseDTO {
        val deltakerIdent = registerService.verifiserAktørTilhørerDeltakelse(id, aktørIdDto.aktorId)

        if (tilgangskontrollService.erSystemBruker()) {
            tilgangskontrollService.krevSystemtilgang()
        } else {
            // ABAC (FAGSAK/UPDATE) krever AksjonspunktType.MANUELL, som kun tildeles enkelte roller.
            // Saksbehandlere som journalfører papirsøknad via ung-sak (OBO) dekkes ikke av dette,
            // så tilgang vurderes i stedet via Tilgangsmaskin (populasjonstilgangskontroll).
            tilgangskontrollService.krevOboTilgangFraGodkjentEksternSystem(
                listOf("ung-sak"),
                PersonIdent.fra(deltakerIdent)
            )
        }

        return registerService.markerSomHarSøkt(id)
    }

}
