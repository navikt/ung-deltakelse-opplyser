package no.nav.ung.deltakelseopplyser.soknad

import no.nav.ung.deltakelseopplyser.AbstractIntegrationTest
import no.nav.ung.deltakelseopplyser.utils.KafkaUtils.leggPåTopic
import no.nav.ung.deltakelseopplyser.utils.MDCUtil
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class UngdomsytelsesøknadKonsumentTest : AbstractIntegrationTest() {

    override val consumerGroupPrefix: String = "ungdomsytelsesøknad-konsument"
    override val consumerGroupTopics: List<String> = listOf("dusseldorf.ungdomsytelse-soknad-cleanup")

    @Test
    fun `skal konsumere søknad`() {
        val søknadId = "49d5cdb9-13be-450f-8327-187a03bed1a3"
        val correlationId = "cd9b224f-b344-480c-8513-f68a19cb7b3a"

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
                    "søknadId": "$søknadId",
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
                "correlationId": "$correlationId",
                "soknadDialogCommitSha": "2024.11.04-09.27-1d1c461",
                "version": 1
              }
            }
        """.trimIndent()

        producer.leggPåTopic(søknadId, søknad, "dusseldorf.ungdomsytelse-soknad-cleanup")

    }
}
