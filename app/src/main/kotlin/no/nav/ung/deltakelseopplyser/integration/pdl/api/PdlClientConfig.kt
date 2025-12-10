package no.nav.ung.deltakelseopplyser.integration.pdl.api

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import no.nav.ung.deltakelseopplyser.integration.common.GraphQLWebClientFactory
import no.nav.ung.deltakelseopplyser.utils.Constants.BEHANLINGSNUMMER
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PdlClientConfig(
    @Value("\${no.nav.gateways.pdl-api-base-url}") private val pdlBaseUrl: String,
    private val graphQLWebClientFactory: GraphQLWebClientFactory,
) {

    @Bean
    fun pdlClient(): GraphQLWebClient = graphQLWebClientFactory.createClient(
        url = "${pdlBaseUrl}/graphql",
        oauth2ClientName = "azure-pdl-api",
        additionalHeaders = mapOf(
            "Tema" to "UNG",
            BEHANLINGSNUMMER to "B950"
        )
    )
}
