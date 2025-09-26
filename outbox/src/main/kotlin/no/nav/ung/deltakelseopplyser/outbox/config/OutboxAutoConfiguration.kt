package no.nav.ung.deltakelseopplyser.outbox.config

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@AutoConfiguration
@ComponentScan(basePackages = ["no.nav.ung.deltakelseopplyser.outbox"])
@EntityScan(basePackages = ["no.nav.ung.deltakelseopplyser.outbox"])
@EnableJpaRepositories(basePackages = ["no.nav.ung.deltakelseopplyser.outbox"])
@EnableScheduling
@ConditionalOnProperty(name = ["outbox.enabled"], havingValue = "true", matchIfMissing = true)
class OutboxAutoConfiguration
