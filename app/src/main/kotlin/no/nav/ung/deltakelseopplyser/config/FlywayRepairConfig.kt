package no.nav.ung.deltakelseopplyser.config

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class FlywayRepairConfig {
    @Bean(initMethod = "migrate")
    fun flyway(
        dataSource: DataSource,
        @Value("\${spring.flyway.locations:classpath:db/migration}") locations: String,
        @Value("\${FLYWAY_REPAIR_ON_FAIL}") flywayRepairOnFail: Boolean
    ): Flyway {
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations(*locations.split(",").toTypedArray())
            .load()
        try {
            flyway.migrate()
        } catch (e: FlywayException) {
            if (flywayRepairOnFail) {
                try {
                    flyway.repair()
                    flyway.migrate()
                } catch (e: FlywayException) {
                    throw e
                }
            } else {
                throw e
            }
        }
        return flyway
    }
}

