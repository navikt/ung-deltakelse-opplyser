package no.nav.ung.deltakelseopplyser

import org.flywaydb.core.Flyway
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean


@SpringBootApplication
class UngDeltakelseOpplyserApplication {

    fun main(args: Array<String>) {
        runApplication<UngDeltakelseOpplyserApplication>(*args)
    }

    @Bean
    fun flywayMigrationStrategy(): FlywayMigrationStrategy {
        return FlywayMigrationStrategy { flyway: Flyway ->
            flyway.repair() // This will repair the Flyway schema history
            flyway.migrate() // Run migrations after repairing
        }
    }
}
