package no.nav.ung.deltakelseopplyser.domene.register.kafka

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.mockk.justRun
import io.mockk.verify
import no.nav.ung.deltakelseopplyser.AbstractIntegrationTest
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramytelseVedtakService
import no.nav.ung.deltakelseopplyser.integration.abac.SifAbacPdpService
import no.nav.ung.deltakelseopplyser.integration.abac.TilgangskontrollService
import no.nav.ung.deltakelseopplyser.utils.KafkaUtils.leggPåTopic
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit

class UngdomsytelseYtelseVedtattKonsumentTest : AbstractIntegrationTest() {

    private companion object {
        const val TOPIC = "k9saksbehandling.ung-vedtakhendelse"
    }

    override val consumerGroupPrefix: String = "ungdomsytelse-vedtak-hendelse-konsument"
    override val consumerGroupTopics: List<String> = listOf(TOPIC)

    @SpykBean
    lateinit var ungdomsprogramytelseVedtakService: UngdomsprogramytelseVedtakService

    @MockkBean(relaxed = true)
    lateinit var sifAbacPdpService: SifAbacPdpService

    @MockkBean(relaxed = true)
    lateinit var tilgangskontrollService: TilgangskontrollService

    @Autowired
    lateinit var konsument: UngdomsytelseYtelseVedtattKonsument

    @Test
    fun `Forventet opphørsvedtak konsumeres og deserialiseres som forventet`() {
        justRun { tilgangskontrollService.krevSystemtilgang() }

        //language=JSON
        val vedtak = """
            {
              "version" : "1.0",
              "aktør" : {
                "verdi" : "9906437824817"
              },
              "vedtattTidspunkt" : "2026-01-20T19:49:09.505",
              "ytelse" : "UNGDOMSYTELSE",
              "saksnummer" : "KV06S",
              "vedtakReferanse" : "ae217bbf-9654-40ce-9767-5feb05f10964",
              "ytelseStatus" : "AVSLUTTET",
              "kildesystem" : "K9SAK",
              "periode" : {
                "fom" : "2026-01-20",
                "tom" : "2026-01-20"
              },
              "tilleggsopplysninger" : null,
              "anvist" : [ ]
            }
        """.trimIndent()

        producer.leggPåTopic("key-1", vedtak, TOPIC)

        await.atMost(10, TimeUnit.SECONDS).untilAsserted {
            verify(exactly = 1) { ungdomsprogramytelseVedtakService.håndterUngdomsprogramytelseOpphørsvedtakForAktør("9906437824817") }
        }
    }
}
