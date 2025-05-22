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

/**
 * Spring Data JPA krever en implementasjon av AuditorAware for å kunne sette auditor.
 * Vi bruker dette til å sette inn hvem som har opprettet og endret en entitet.
 *
 * Vi bruker SpringTokenValidationContextHolder for å hente ut auditor fra token.
 */
@Component
class AuditorAwareImpl(
    private val tokenValidationContextHolder: SpringTokenValidationContextHolder,
) : AuditorAware<String> {

    override fun getCurrentAuditor(): Optional<String> {
        val auditor = tokenValidationContextHolder.auditor()
        return Optional.of(auditor)
    }

    private fun SpringTokenValidationContextHolder.auditor(): String {
        if (!finnesGyldigTokenIContext()) {
            return systemAuditor()
        }

        val jwtToken = gyldigToken()

        return when {
            jwtToken.erTokenxIssuer() -> {
                deltakerAuditor()
            }

            jwtToken.erAzureIssuer() -> {
                // TODO:  Skill mellom veileder og system
                veilederAuditor()
            }

            else -> {
                systemAuditor()
            }
        }
    }

    private fun systemAuditor() = "system"

    private fun SpringTokenValidationContextHolder.veilederAuditor() = "${navIdent()} (veileder)"

    private fun SpringTokenValidationContextHolder.deltakerAuditor() = "${personIdent()} (deltaker)"

    /**
     * Sjekker om det finnes en gyldig token i context.
     */
    fun finnesGyldigTokenIContext(): Boolean {
        return runCatching { tokenValidationContextHolder.getTokenValidationContext() }
            .fold(
                onSuccess = { it.firstValidToken != null },
                onFailure = { false }
            )
    }
}
