package no.nav.ung.deltakelseopplyser.domene.register.ekstern

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.sif.abac.kontrakt.person.PersonIdent
import no.nav.ung.deltakelseopplyser.audit.EventClassId
import no.nav.ung.deltakelseopplyser.audit.SporingsloggService
import no.nav.ung.deltakelseopplyser.config.Issuers
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService
import no.nav.ung.deltakelseopplyser.integration.abac.TilgangskontrollService
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakelseSjekk
import no.nav.ung.deltakelseopplyser.kontrakt.ekstern.DeltakerIdent
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/ekstern/deltakelse")
@RequiredIssuers(
    ProtectedWithClaims(issuer = Issuers.AZURE)
)
@Tag(
    name = "Ekstern deltakelse",
    description = """API for å sjekke om en bruker er aktiv deltaker i ungdomsprogrammet.
        Krever veileder sitt OBO-token – systemtoken (maskin-til-maskin) aksepteres ikke.
        Returnerer alltid HTTP 200 – sjekk feltet 'erDeltaker' for svar."""
)
class EksternDeltakelseController(
    private val sporingsloggService: SporingsloggService,
    private val tilgangskontrollService: TilgangskontrollService,
    private val registerService: UngdomsprogramregisterService,
) {

    @PostMapping(
        "/sjekk",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(
        summary = "Sjekk om bruker er aktiv deltaker i ungdomsprogrammet",
        description = """Returnerer om brukeren er aktiv deltaker i ungdomsprogrammet og eventuelt perioden.
            En periode regnes som aktiv dersom tilOgMed er null (åpen periode) eller satt i fremtiden.
            Krever veileder sitt OBO-token – systemtoken aksepteres ikke.
            Kallet må komme fra et godkjent system (sjekkes via azp-claim).
            Diskresjonskode (kode 6/7) og egne-ansatt-sjekk utføres via ABAC."""
    )
    @ResponseStatus(HttpStatus.OK)
    fun sjekkDeltakelse(@RequestBody deltakerIdent: DeltakerIdent): DeltakelseSjekk {
        val personIdent = PersonIdent.fra(deltakerIdent.ident)

        tilgangskontrollService.krevOboTilgangFraGodkjentEksternSystem(
            listOf(
                "veilarboppfolging",
                "azure-token-generator" // TODO: Fjern før merge til prod.
            ),
            personIdent
        )

        return registerService.sjekkAktivDeltakelse(deltakerIdent.ident)
            .also {
                sporingsloggService.logg(
                    url = "/ekstern/deltakelse/sjekk",
                    beskrivelse = "Sjekket om bruker er aktiv deltaker i ungdomsprogrammet",
                    bruker = personIdent,
                    eventClassId = EventClassId.AUDIT_ACCESS
                )
            }
    }
}
