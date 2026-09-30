package no.nav.ung.deltakelseopplyser.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SwaggerConfigurationTest {

    @Test
    fun `openAPI includes OBO token schemes and security requirements`() {
        val swaggerConfiguration = SwaggerConfiguration(
            authorizationUrl = "https://login.example.com/authorize",
            tokenUrl = "https://login.example.com/token",
            apiScope = "api://my-app/.default",
            oboAudience = "dev-gcp:k9saksbehandling:ung-deltakelse-opplyser",
            tokenXTokenGeneratorUrl = "https://tokenx-token-generator.intern.dev.nav.no",
            azureTokenGeneratorUrl = "https://azure-token-generator.intern.dev.nav.no"
        )

        val openApi = swaggerConfiguration.openAPI()
        val securitySchemes = openApi.components.securitySchemes

        assertThat(securitySchemes).containsKeys("Authorization", "entraObo", "oauth2")
        assertThat(securitySchemes["Authorization"]?.description)
            .contains("tokenx-token-generator.intern.dev.nav.no/api/obo?aud=dev-gcp:k9saksbehandling:ung-deltakelse-opplyser")
        assertThat(securitySchemes["entraObo"]?.description)
            .contains("azure-token-generator.intern.dev.nav.no/api/obo?aud=dev-gcp:k9saksbehandling:ung-deltakelse-opplyser")

        assertThat(openApi.security).anySatisfy { requirement ->
            assertThat(requirement).containsKey("Authorization")
        }
        assertThat(openApi.security).anySatisfy { requirement ->
            assertThat(requirement).containsKey("entraObo")
        }
        assertThat(openApi.security).anySatisfy { requirement ->
            assertThat(requirement).containsEntry("oauth2", listOf("api://my-app/.default"))
        }
    }
}

