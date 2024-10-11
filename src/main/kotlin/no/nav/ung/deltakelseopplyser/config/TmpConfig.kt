package no.nav.ung.deltakelseopplyser.config

import org.flywaydb.core.Flyway
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TmpConfig {
    @Bean
    fun flywayMigrationStrategy(): FlywayMigrationStrategy {
        return FlywayMigrationStrategy { flyway: Flyway ->
            flyway.repair() // This will repair the Flyway schema history
            flyway.migrate() // Run migrations after repairing
        }
    }
}
