package no.nav.ung.deltakelseopplyser.config

import no.nav.familie.prosessering.PropertiesWrapperTilStringConverter
import no.nav.familie.prosessering.StringTilPropertiesWrapperConverter
import no.nav.familie.prosessering.config.ProsesseringInfoProvider
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Configuration
@ComponentScan("no.nav.familie.prosessering")
@EnableJdbcRepositories("no.nav.familie.prosessering")
class ProsesseringConfiguration : AbstractJdbcConfiguration() {

    @Bean
    fun transactionTemplate(transactionManager: JpaTransactionManager): TransactionTemplate {
        return TransactionTemplate(transactionManager)
    }

    @Bean
    override fun userConverters(): List<*> =
        listOf(
            PropertiesWrapperTilStringConverter(),
            StringTilPropertiesWrapperConverter()
        )

    @Bean
    fun prosesseringInfoProvider(
        @Value("\${prosessering.rolle}") prosesseringRolle: String,
    ) = object : ProsesseringInfoProvider {
        override fun hentBrukernavn(): String =
            try {
                SpringTokenValidationContextHolder().getTokenValidationContext().getClaims("azuread")
                    .getStringClaim("preferred_username")
            } catch (e: Exception) {
                "VL"
            }

        override fun harTilgang(): Boolean = grupper().contains(prosesseringRolle)
    }

    private fun grupper(): List<String> =
        try {
            SpringTokenValidationContextHolder()
                .getTokenValidationContext()
                .getClaims("azuread")
                .get("groups") as List<String>? ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
}
