package no.nav.ung.deltakelseopplyser.diagnostikk

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.sif.abac.kontrakt.abac.BeskyttetRessursActionAttributt
import no.nav.ung.deltakelseopplyser.config.Issuers
import no.nav.ung.deltakelseopplyser.config.Issuers.TOKEN_X
import no.nav.ung.deltakelseopplyser.config.TxConfiguration.Companion.TRANSACTION_MANAGER
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.oppgave.OppgaveMapperService
import no.nav.ung.deltakelseopplyser.domene.oppgave.OppgaveService
import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseDAO
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramDeltakelseRepository
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService.Companion.mapToDTO
import no.nav.ung.deltakelseopplyser.integration.abac.SifAbacPdpService
import no.nav.ung.deltakelseopplyser.integration.abac.TilgangskontrollService
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveDTO
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseDTO
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseKomposittDTO
import no.nav.ung.deltakelseopplyser.utils.personIdent
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.ErrorResponseException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
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
) {

    @GetMapping("/hent/deltakelse", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Hent deltakelse gitt id")
    @ResponseStatus(HttpStatus.OK)
    fun hentDeltakelse(@PathVariable id: UUID): DeltakelseDTO {
        tilgangskontrollService.krevDriftsTilgang(BeskyttetRessursActionAttributt.READ)
        val deltakelse: Optional<DeltakelseDAO> = deltakelseRepository.findById(id)
        return deltakelse.map { it.mapToDTO() }.orElseThrow { IllegalArgumentException("Fant ikke deltakelse: $id") }
    }


}
