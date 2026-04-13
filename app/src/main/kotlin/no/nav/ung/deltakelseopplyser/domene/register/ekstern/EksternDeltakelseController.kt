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
        Støtter både systemtoken (maskin-til-maskin, idtyp=app) og OBO-token (på vegne av veileder).
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
            Støtter systemtoken (maskin-til-maskin) og OBO-token (på vegne av veileder).
            Kallet må komme fra et godkjent system (sjekkes via azp-claim).
            Ved OBO-token utføres diskresjonskode (kode 6/7) og egne-ansatt-sjekk via tilgangsmaskin, og det skrives sporingslogg."""
    )
    @ResponseStatus(HttpStatus.OK)
    fun sjekkDeltakelse(@RequestBody deltakerIdent: DeltakerIdent): DeltakelseSjekk {
        val personIdent = PersonIdent.fra(deltakerIdent.ident)
        val erSystemkall = tilgangskontrollService.erSystemBruker()

        if (erSystemkall) {
            tilgangskontrollService.krevSystemtilgang(listOf("veilarboppfolging"))
        } else {
            tilgangskontrollService.krevOboTilgangFraGodkjentEksternSystem(
                listOf("veilarboppfolging"),
                personIdent
            )
        }

        return registerService.sjekkAktivDeltakelse(deltakerIdent.ident)
            .also {
                if (!erSystemkall) {
                    sporingsloggService.logg(
                        url = "/ekstern/deltakelse/sjekk",
                        beskrivelse = "Sjekket om bruker er aktiv deltaker i ungdomsprogrammet",
                        bruker = personIdent,
                        eventClassId = EventClassId.AUDIT_ACCESS
                    )
                }
            }
    }
}
