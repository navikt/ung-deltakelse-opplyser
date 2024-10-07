package no.nav.ung.deltakelseopplyser.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders


@Configuration
class SwaggerConfiguration {

    @Bean
    fun openAPI(): OpenAPI {
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
            )
            .addSecurityItem(
                SecurityRequirement()
                    .addList("Authorization")
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
}
