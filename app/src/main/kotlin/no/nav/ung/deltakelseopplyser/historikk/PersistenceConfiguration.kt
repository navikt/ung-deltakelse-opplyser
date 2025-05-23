package no.nav.ung.deltakelseopplyser.historikk

import no.nav.ung.deltakelseopplyser.historikk.AuditorAwareImpl.Companion.AUDITOR_AWARE_IMPL_BEAN_NAME
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@Configuration
@EnableJpaAuditing(auditorAwareRef = AUDITOR_AWARE_IMPL_BEAN_NAME)
class PersistenceConfiguration
