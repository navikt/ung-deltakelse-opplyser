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
    @Value("\${springdoc.oAuthFlow.apiScope}") val apiScope: String
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
    fun diagnostikkOpenApi(): GroupedOpenApi {
        val packagesToscan = arrayOf(
            "no.nav.ung.deltakelseopplyser.diagnostikk",
        )
        return GroupedOpenApi.builder()
            .group("diagnostikk").packagesToScan(*packagesToscan)
            .build()
    }

    @Bean
    fun openAPI(): OpenAPI {
        // use Reusable Enums for Swagger generation:
        // see https://springdoc.org/#how-can-i-apply-enumasref-true-to-all-enums
        io.swagger.v3.core.jackson.ModelResolver.enumsAsRef = true;

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
                    .addSecuritySchemes("oauth2", azureLogin())
            )
            .addSecurityItem(
                SecurityRequirement()
                    .addList("Authorization")
                    .addList("oauth2", listOf("read", "write"))
            )
    }

    private fun tokenXApiToken(): SecurityScheme {
        val audience = "dev-gcp:k9saksbehandling:ung-deltakelse-opplyser"

        return SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .name(HttpHeaders.AUTHORIZATION)
            .scheme("bearer")
            .bearerFormat("JWT")
            .`in`(SecurityScheme.In.HEADER)
            .description(
                """Eksempel på verdi som skal inn i Value-feltet (Bearer trengs altså ikke å oppgis): 'eyAidH...'
                For nytt token -> https://tokenx-token-generator.intern.dev.nav.no/api/obo?aud=$audience
            """.trimMargin()
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
