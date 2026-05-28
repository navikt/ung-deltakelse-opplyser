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
import no.nav.ung.deltakelseopplyser.kontrakt.ekstern.AlleDeltakelserResponseDTO
import no.nav.ung.deltakelseopplyser.kontrakt.ekstern.DeltakelseInfoDTO
import no.nav.ung.deltakelseopplyser.kontrakt.ekstern.DeltakelsePeriodeDTO
import no.nav.ung.deltakelseopplyser.kontrakt.ekstern.DeltakerIdent
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
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
    description = """API for oppslag av deltakelse i ungdomsprogrammet.
        Støtter både OBO-token (veileder) og systemtoken / M2M (maskin-til-maskin)."""
)
class EksternDeltakelseController(
    private val sporingsloggService: SporingsloggService,
    private val tilgangskontrollService: TilgangskontrollService,
    private val registerService: UngdomsprogramregisterService,
    environment: Environment,
) {

    private val godkjenteApplikasjonerSjekk: List<String> = buildList {
        add("veilarboppfolging")
        if (environment.activeProfiles.contains("dev-gcp")) {
            add("azure-token-generator")
        }
    }

    private val godkjenteApplikasjonerAlle: List<String> = buildList {
        add("veilarbportefolje")
        if (environment.activeProfiles.contains("dev-gcp")) {
            add("azure-token-generator")
        }
    }


    @PostMapping(
        "/sjekk",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(
        summary = "Sjekk om bruker er aktiv deltaker i ungdomsprogrammet",
        description = """Returnerer om brukeren er aktiv deltaker i ungdomsprogrammet og eventuelt perioden.
            En periode regnes som aktiv dersom tilOgMed er null (åpen periode) eller satt i fremtiden.
            Støtter både OBO-token (veileder) og systemtoken / M2M (maskin-til-maskin).
            Kallet må komme fra et godkjent system (sjekkes via azp-claim).
            For OBO-token: tilgangskontroll via Tilgangsmaskin og sporingslogg.
            For systemtoken: kun applikasjonsvalidering, ingen sporingslogg."""
    )
    @ResponseStatus(HttpStatus.OK)
    fun sjekkDeltakelse(@RequestBody deltakerIdent: DeltakerIdent): DeltakelseSjekk {
        val personIdent = PersonIdent.fra(deltakerIdent.ident)
        val erSystemkall = tilgangskontrollService.erSystemBruker()

        if (erSystemkall) {
            tilgangskontrollService.krevSystemtilgang(godkjenteApplikasjonerSjekk)
        } else {
            tilgangskontrollService.krevOboTilgangFraGodkjentEksternSystem(
                godkjenteApplikasjonerSjekk,
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

    @GetMapping(
        "/alle",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(
        summary = "Hent alle deltakelser i ungdomsprogrammet",
        description = """Returnerer alle (ikke-slettede) deltakelser i ungdomsprogrammet med periode og forlengelsesinfo.
            Kun tilgjengelig for systemtoken (M2M / client_credentials).
            Kallet må komme fra et godkjent system (sjekkes via azp-claim)."""
    )
    @ResponseStatus(HttpStatus.OK)
    fun hentAlleDeltakelser(): AlleDeltakelserResponseDTO {

        tilgangskontrollService.krevSystemtilgang(godkjenteApplikasjonerAlle)

        val deltakelser = registerService.hentAlleDeltakelser()

        return AlleDeltakelserResponseDTO(
            deltakelser = deltakelser.map { deltakelse ->
                DeltakelseInfoDTO(
                    deltakelseId = deltakelse.id!!,
                    deltakerIdent = deltakelse.deltaker.deltakerIdent,
                    periode = DeltakelsePeriodeDTO(
                        fraOgMed = deltakelse.fraOgMed,
                        tilOgMed = deltakelse.tilOgMed,
                        harForlengetPeriode = deltakelse.harForlengetPeriode,
                        periodeMaksDato = deltakelse.periodeMaksDato,
                    ),
                )
            }
        )
    }
}
