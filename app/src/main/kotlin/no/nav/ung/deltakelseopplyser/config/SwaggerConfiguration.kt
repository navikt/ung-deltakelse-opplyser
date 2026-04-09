package no.nav.ung.deltakelseopplyser.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.OAuthFlows
import io.swagger.v3.oas.models.security.Scopes
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders


@Configuration
class SwaggerConfiguration(
    @Value("\${springdoc.oAuthFlow.authorizationUrl}") val authorizationUrl: String,
    @Value("\${springdoc.oAuthFlow.tokenUrl}") val tokenUrl: String,
    @Value("\${springdoc.oAuthFlow.apiScope}") val apiScope: String,
    @Value("\${springdoc.oAuthFlow.oboAudience:\${NAIS_CLUSTER_NAME:dev-gcp}:\${NAIS_NAMESPACE:k9saksbehandling}:\${NAIS_APP_NAME:ung-deltakelse-opplyser}}") val oboAudience: String,
    @Value("\${springdoc.oAuthFlow.tokenXTokenGeneratorUrl:https://tokenx-token-generator.intern.dev.nav.no}") val tokenXTokenGeneratorUrl: String,
    @Value("\${springdoc.oAuthFlow.azureTokenGeneratorUrl:https://azure-token-generator.intern.dev.nav.no}") val azureTokenGeneratorUrl: String
) {

    @Bean
    fun deltakerOpenApi(): GroupedOpenApi {
        val packagesToscan = arrayOf(
            "no.nav.ung.deltakelseopplyser.domene.register.deltaker",
        )
        return GroupedOpenApi.builder()
            .group("deltaker").packagesToScan(*packagesToscan)
            .build()
    }

    @Bean
    fun veilederOpenApi(): GroupedOpenApi {
        val packagesToscan = arrayOf(
            "no.nav.ung.deltakelseopplyser.domene.register.veileder"
        )
        return GroupedOpenApi.builder()
            .group("veileder").packagesToScan(*packagesToscan)
            .build()
    }

    @Bean
    fun ungSakOpenApi(): GroupedOpenApi {
        val packagesToscan = arrayOf(
            "no.nav.ung.deltakelseopplyser.domene.register.ungsak"
        )
        return GroupedOpenApi.builder()
            .group("ung-sak").packagesToScan(*packagesToscan)
            .build()
    }

    @Bean
    fun eksternOpenApi(): GroupedOpenApi {
        val packagesToscan = arrayOf(
            "no.nav.ung.deltakelseopplyser.domene.register.ekstern"
        )
        return GroupedOpenApi.builder()
            .group("ekstern").packagesToScan(*packagesToscan)
            .build()
    }

    @Bean
    fun driftOpenApi(): GroupedOpenApi {
        val packagesToscan = arrayOf(
            "no.nav.ung.deltakelseopplyser.drift",
        )
        return GroupedOpenApi.builder()
            .group("drift").packagesToScan(*packagesToscan)
            .build()
    }

    @Bean
    fun openAPI(): OpenAPI {
        // use Reusable Enums for Swagger generation:
        // see https://springdoc.org/#how-can-i-apply-enumasref-true-to-all-enums
        io.swagger.v3.core.jackson.ModelResolver.enumsAsRef = true

        return OpenAPI()
            .info(
                Info()
                    .title("Ungdomprogramregister API")
                    .description("API spesifikasjon for Ungdomsprogramregister")
                    .version("v1.0.0")
            )
            .externalDocs(
                ExternalDocumentation()
                    .description("Ungdomprogramregister API GitHub repository")
                    .url("https://github.com/navikt/ung-deltaker-opplyser")
            )
            .components(
                Components()
                    .addSecuritySchemes("Authorization", tokenXApiToken())
                    .addSecuritySchemes("entraObo", entraOboApiToken())
                    .addSecuritySchemes("oauth2", azureLogin())
            )
            // OpenAPI security items are OR across entries.
            .addSecurityItem(SecurityRequirement().addList("Authorization"))
            .addSecurityItem(SecurityRequirement().addList("entraObo"))
            .addSecurityItem(SecurityRequirement().addList("oauth2", listOf(apiScope)))
    }

    private fun tokenXApiToken(): SecurityScheme {
        return SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .name(HttpHeaders.AUTHORIZATION)
            .scheme("bearer")
            .bearerFormat("JWT")
            .`in`(SecurityScheme.In.HEADER)
            .description(
                """Brukes for deltaker-endepunkter (TokenX).
                Eksempel pa verdi i Value-feltet: 'eyAidH...'
                Generer nytt token: $tokenXTokenGeneratorUrl/api/obo?aud=$oboAudience
            """.trimIndent()
            )
    }

    private fun entraOboApiToken(): SecurityScheme {
        return SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .name(HttpHeaders.AUTHORIZATION)
            .scheme("bearer")
            .bearerFormat("JWT")
            .`in`(SecurityScheme.In.HEADER)
            .description(
                """Brukes for veileder-, ung-sak-, ekstern- og drift-endepunkter (Entra ID OBO).
                Eksempel pa verdi i Value-feltet: 'eyAidH...'
                Generer nytt token: $azureTokenGeneratorUrl/api/obo?aud=$oboAudience
            """.trimIndent()
            )
    }

    private fun azureLogin(): SecurityScheme {
        return SecurityScheme()
            .name("oauth2")
            .type(SecurityScheme.Type.OAUTH2)
            .scheme("oauth2")
            .`in`(SecurityScheme.In.HEADER)
            .flows(
                OAuthFlows()
                    .authorizationCode(
                        OAuthFlow().authorizationUrl(authorizationUrl)
                            .tokenUrl(tokenUrl)
                            .scopes(Scopes().addString(apiScope, "read,write"))
                    )
            )
    }
}
