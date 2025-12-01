package no.nav.ung.deltakelseopplyser.config

import no.nav.k9.felles.konfigurasjon.env.Environment
import no.nav.ung.deltakelseopplyser.domene.soknad.kafka.UngdomsytelsesøknadKonsumentConfiguration
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class FlywayRepairConfig {
        companion object {
        private val logger = LoggerFactory.getLogger(FlywayRepairConfig::class.java)
    }
    @Bean(initMethod = "migrate")
    fun flyway(
        dataSource: DataSource,
        @Value("\${spring.flyway.locations:classpath:db/migration}") locations: String,
        @Value("\${FLYWAY_REPAIR_ON_FAIL}") flywayRepairOnFail: Boolean
    ): Flyway {

        val isProd = Environment.current().isProd;
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations(*locations.split(",").toTypedArray())
            .load()
        try {
            flyway.migrate()
        } catch (e: FlywayException) {
            // Skal aldri kjøre i prod
            if (!isProd && flywayRepairOnFail) {
                try {
                    logger.warn("Flyway migrering feilet, prøver repair og migrate på nytt", e)
                    logger.warn("Kjører flyway repair. Alle nye skript vil bli ignorert og checksum vil bli oppdatert for å matche migreringer som finnes i koden.")
                    flyway.repair()
                    logger.warn("Repair er ferdig kjørt, prøver ny migrering")
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

