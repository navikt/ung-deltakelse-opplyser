package no.nav.ung.deltakelseopplyser.historikk

import org.springframework.data.domain.AuditorAware
import org.springframework.stereotype.Component
import java.util.*

@Component
class AuditorAwareImpl: AuditorAware<String> {
    override fun getCurrentAuditor(): Optional<String> {
        return Optional.of("system") // TODO: Implement a proper auditor aware implementation
    }
}
