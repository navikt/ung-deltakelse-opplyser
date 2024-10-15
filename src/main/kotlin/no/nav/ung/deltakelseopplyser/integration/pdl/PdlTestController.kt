package no.nav.ung.deltakelseopplyser.integration.pdl

import no.nav.pdl.generated.hentident.Identliste
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// TODO: Slett dette før lansering. Dette er kun for å teste PDL-integrasjonen.
@RestController
@RequestMapping("/pdl-test")
@Unprotected
class PdlTestController(
    private val pdlService: PdlService,
) {

    @RequestMapping("/identer/{ident}")
    fun hentIdenter(@PathVariable ident: String): Identliste {
        return pdlService.hentIdenter(ident)
    }
}
