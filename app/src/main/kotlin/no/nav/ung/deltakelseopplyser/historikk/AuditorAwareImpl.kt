package no.nav.ung.deltakelseopplyser.historikk

import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.ung.deltakelseopplyser.config.Issuers
import no.nav.ung.deltakelseopplyser.historikk.AuditorAwareImpl.Companion.AUDITOR_AWARE_IMPL_BEAN_NAME
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
@Component(value = AUDITOR_AWARE_IMPL_BEAN_NAME)
class AuditorAwareImpl(
    private val tokenValidationContextHolder: SpringTokenValidationContextHolder,
    private val multiIssuerConfiguration: MultiIssuerConfiguration,
) : AuditorAware<String> {

    companion object{
        const val AUDITOR_AWARE_IMPL_BEAN_NAME = "auditorAwareImpl"
    }

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
                when {
                    jwtToken.erAzureSystemToken() -> systemAuditor()
                    else -> veilederAuditor()
                }
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
    private fun finnesGyldigTokenIContext(): Boolean {
        return runCatching { tokenValidationContextHolder.getTokenValidationContext() }
            .fold(
                onSuccess = { it.firstValidToken != null },
                onFailure = { false }
            )
    }

    private fun JwtToken.erTokenxIssuer(): Boolean {
        return multiIssuerConfiguration.issuers[Issuers.TOKEN_X]?.metadata?.issuer?.value == issuer
    }

    private fun JwtToken.erAzureSystemToken(): Boolean {
        return erAzureIssuer() && jwtTokenClaims.getStringClaim("idtyp") == "app"
    }

    private fun JwtToken.erAzureIssuer(): Boolean {
        return multiIssuerConfiguration.issuers[Issuers.AZURE]?.metadata?.issuer?.value == issuer
    }
}
