package no.nav.ung.deltakelseopplyser.historikk

import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.ung.deltakelseopplyser.utils.erAzureIssuer
import no.nav.ung.deltakelseopplyser.utils.erTokenxIssuer
import no.nav.ung.deltakelseopplyser.utils.gyldigToken
import no.nav.ung.deltakelseopplyser.utils.navIdent
import no.nav.ung.deltakelseopplyser.utils.personIdent
import org.springframework.data.domain.AuditorAware
import org.springframework.stereotype.Component
import java.util.*

@Component
class AuditorAwareImpl(
    private val tokenValidationContextHolder: SpringTokenValidationContextHolder,
) : AuditorAware<String> {

    override fun getCurrentAuditor(): Optional<String> {
        val auditor = tokenValidationContextHolder.auditor()
        return Optional.of(auditor)
    }

    private fun SpringTokenValidationContextHolder.auditor(): String {
        val jwtToken = gyldigToken()
        val erTokenxIssuer = jwtToken.erTokenxIssuer()
        val erAzureIssuer = jwtToken.erAzureIssuer()

        return if (erTokenxIssuer) {
            "${personIdent()} (deltaker)"
        } else if (erAzureIssuer) {
            "${navIdent()} (veileder)"
        } else {
            "system"
        }
    }
}
