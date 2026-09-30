package no.nav.ung.deltakelseopplyser.config

import no.nav.ung.deltakelseopplyser.AbstractIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.get

/**
 * Regresjonstest for produksjonsfeilen der GET /v3/api-docs/deltaker returnerte 500
 * ("Null key for a Map not allowed in JSON"). Rotårsaken er en kjent bug i swagger-core
 * 2.2.47 (se [SwaggerConfiguration.nullKeyCleanupCustomizer]), som trigges av kombinasjonen
 * av @JsonUnwrapped og enumsAsRef for DeltakelseDTO/DeltakelseKomposittDTO.
 *
 * Kjører gjennom hele springdoc/swagger-pipelinen (i motsetning til en isolert
 * ModelConverters-test), siden feilen kun oppstod i springdoc sin faktiske
 * schema-serialisering og ikke i en generell swagger-core schema-resolve.
 */
@TestPropertySource(properties = ["SWAGGER_ENABLED=true"])
class OpenApiDocsIntegrationTest : AbstractIntegrationTest() {

    override val consumerGroupPrefix: String
        get() = "openapi-docs-test"

    override val consumerGroupTopics: List<String>
        get() = emptyList()

    @Test
    fun `v3 api-docs deltaker returnerer 200`() {
        mockMvc.get("/v3/api-docs/deltaker") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
        }
    }
}
