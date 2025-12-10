package no.nav.ung.deltakelseopplyser.integration.nom.api

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import no.nav.ung.deltakelseopplyser.integration.common.GraphQLWebClientFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class NomApiClientConfig(
    @Value("\${no.nav.gateways.nom-api-base-url}") private val nomApiBaseUrl: String,
    private val graphQLWebClientFactory: GraphQLWebClientFactory,
) {

    @Bean
    fun nomApiClient(): GraphQLWebClient = graphQLWebClientFactory.createClient(
        url = "${nomApiBaseUrl}/graphql",
        oauth2ClientName = "azure-nom-api",
    )
}
