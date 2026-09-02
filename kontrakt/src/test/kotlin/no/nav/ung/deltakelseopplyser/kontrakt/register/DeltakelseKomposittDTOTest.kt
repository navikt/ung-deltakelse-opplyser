package no.nav.ung.deltakelseopplyser.kontrakt.register

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class DeltakelseKomposittDTOTest {

    private val mapper = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())

    @Suppress("DEPRECATION")
    @Test
    fun `serialisering av DeltakelseKomposittDTO gir kun ett status-felt`() {
        val dto = DeltakelseKomposittDTO(
            deltakelse = DeltakelseDTO(
                id = UUID.randomUUID(),
                deltaker = DeltakerDTO(deltakerIdent = "12345678910"),
                fraOgMed = LocalDate.parse("2024-01-01"),
                periodeMaksDato = LocalDate.parse("2025-01-01"),
            )
        )

        val json = mapper.writeValueAsString(dto)

        // Uten @JsonIgnore på den lokale status-getteren i DeltakelseKomposittDTO ga Jackson et
        // duplikat "status"-felt i JSON-output (ett fra @JsonUnwrapped deltakelse, ett fra den
        // lokale getteren), som er ugyldig JSON.
        val statusKeyOccurrences = Regex("\"status\"\\s*:").findAll(json).count()
        assertThat(statusKeyOccurrences).isEqualTo(1)
    }
}
