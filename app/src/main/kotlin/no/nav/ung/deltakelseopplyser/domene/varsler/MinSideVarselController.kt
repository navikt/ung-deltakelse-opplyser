package no.nav.ung.deltakelseopplyser.domene.varsler

import io.swagger.v3.oas.annotations.Operation
import jakarta.transaction.Transactional
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.tms.varsel.action.Tekst
import no.nav.ung.deltakelseopplyser.config.Issuers.TOKEN_X
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// TODO: Husk å slette denne controlleren før merge til master.
@RestController
@RequestMapping("/min-side/varsel")
@RequiredIssuers(
    ProtectedWithClaims(
        issuer = TOKEN_X,
        claimMap = ["acr=Level4", "acr=idporten-loa-high"],
        combineWithOr = true
    )
)
class MinSideVarselController(
    private val mineSiderVarselService: MineSiderVarselService,
) {

    @PostMapping("/opprett/oppgave", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Opprett en oppgave på Min side")
    @Transactional
    fun meldInnDeltaker(@RequestBody opprettOppgaveDTO: OpprettOppgaveDTO) {

        mineSiderVarselService.opprettOppgve(
            oppgaveId = opprettOppgaveDTO.oppgaveId,
            deltakerIdent = opprettOppgaveDTO.deltakerIdent,
            oppgavetekster = opprettOppgaveDTO.oppgavetekster,
            oppgavelenke = opprettOppgaveDTO.oppgavelenke
        )
    }

    @DeleteMapping("/inaktiver/oppgave/{oppgaveId}")
    @Operation(summary = "Inaktiver en oppgave på Min side")
    fun inaktiverOppgave(@PathVariable oppgaveId: String) {
        mineSiderVarselService.deaktiverOppgave(oppgaveId)
    }

    data class OpprettOppgaveDTO(
        val oppgaveId: String,
        val deltakerIdent: String,
        val oppgavetekster: List<Tekst>,
        val oppgavelenke: String,
    )
}
