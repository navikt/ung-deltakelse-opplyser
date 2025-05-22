package no.nav.ung.deltakelseopplyser.historikk

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
class PersistenceConfiguration
