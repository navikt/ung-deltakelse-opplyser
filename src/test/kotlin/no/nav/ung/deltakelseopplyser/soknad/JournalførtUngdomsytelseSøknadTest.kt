package no.nav.ung.deltakelseopplyser.soknad

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest

@JsonTest
class JournalførtUngdomsytelseSøknadTest {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private companion object {

        //language=JSON
        val søknad = """
            {
              "data": {
                "journalførtMelding": {
                  "type": "JournalfortSøknad",
                  "journalpostId": "671161658",
                  "søknad": {
                    "mottattDato": "2024-11-04T10:57:18.634Z",
                    "språk": "nb",
                    "søker": {
                      "norskIdentitetsnummer": "0249*******"
                    },
                    "søknadId": "49d5cdb9-13be-450f-8327-187a03bed1a3",
                    "versjon": "1.0.0",
                    "ytelse": {
                      "type": "UNGDOMSYTELSE",
                      "inntekt": 0,
                      "søknadsperiode": [
                        "2024-11-04/2024-11-21"
                      ]
                    },
                    "begrunnelseForInnsending": {
                      "tekst": null
                    },
                    "journalposter": [],
                    "kildesystem": "søknadsdialog"
                  }
                }
              },
              "metadata": {
                "correlationId": "cd9b224f-b344-480c-8513-f68a19cb7b3a",
                "soknadDialogCommitSha": "2024.11.04-09.27-1d1c461",
                "version": 1
              }
            }
        """.trimIndent()
    }

    @Test
    fun `Deserialisering av UngdomsytelseSøknadTopicEntry feiler ikke`() {
        assertDoesNotThrow {
            objectMapper.readValue(søknad, UngdomsytelseSøknadTopicEntry::class.java)
        }
    }
}
