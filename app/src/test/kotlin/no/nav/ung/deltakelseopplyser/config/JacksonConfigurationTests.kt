package no.nav.ung.deltakelseopplyser.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import java.time.ZonedDateTime

@JsonTest
class JacksonConfigurationTests {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun contextLoads() {
    }

    @Test
    fun `Deserialisering til ZonedDateTime feiler ikke`() {
        assertDoesNotThrow {
            objectMapper.readValue("\"2024-11-04T10:57:18.428198503Z\"", ZonedDateTime::class.java)
        }
    }
}
