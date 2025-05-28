package no.nav.ung.deltakelseopplyser.domene.soknad.kafka

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.hypersistence.utils.hibernate.type.range.Range
import io.mockk.every
import io.mockk.verify
import no.nav.ung.deltakelseopplyser.AbstractIntegrationTest
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDAO
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.minside.mikrofrontend.MikrofrontendService
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramDeltakelseDAO
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramDeltakelseRepository
import no.nav.ung.deltakelseopplyser.domene.soknad.UngdomsytelsesøknadService
import no.nav.ung.deltakelseopplyser.domene.soknad.repository.SøknadRepository
import no.nav.ung.deltakelseopplyser.utils.KafkaUtils.leggPåTopic
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.time.LocalDate
import java.util.*

class UngdomsytelsesøknadKonsumentTest : AbstractIntegrationTest() {

    private companion object {
        const val TOPIC = "dusseldorf.ungdomsytelse-soknad-cleanup"
    }

    override val consumerGroupPrefix: String = "ungdomsytelsesoknad-konsument"
    override val consumerGroupTopics: List<String> = listOf("dusseldorf.ungdomsytelse-soknad-cleanup")

    @Autowired
    lateinit var søknadRepository: SøknadRepository

    @SpykBean
    lateinit var ungdomsytelsesøknadService: UngdomsytelsesøknadService

    @MockkBean
    lateinit var deltakerService: DeltakerService

    @MockkBean
    lateinit var deltakelseRepository: UngdomsprogramDeltakelseRepository

    @MockkBean
    lateinit var mikrofrontendService: MikrofrontendService

    @Test
    fun `Forventer at listener forsøker på nytt ved feil`() {
        val søknadId = "49d5cdb9-13be-450f-8327-187a03bed1a3"
        val correlationId = "cd9b224f-b344-480c-8513-f68a19cb7b3a"
        val søkerIdent = "12834619705"

        // Gitt deltaker...
        every { deltakerService.hentDeltakterIder(any()) } returns listOf(UUID.randomUUID())

        // med eksisterende deltakelse...
        every { deltakelseRepository.finnDeltakelseSomStarter(any(), any()) } returns UngdomsprogramDeltakelseDAO(
            id = UUID.randomUUID(),
            deltaker = DeltakerDAO(
                deltakerIdent = søkerIdent,
                id = UUID.randomUUID()
            ),
            periode = Range.openInfinite(LocalDate.now()),
            søktTidspunkt = null
        )


        every { deltakelseRepository.save(ofType(UngdomsprogramDeltakelseDAO::class)) }
            .answers { firstArg<UngdomsprogramDeltakelseDAO>() }

        every { mikrofrontendService.sendOgLagre(any()) } throws RuntimeException("Simulert feil ved publisering av mikrofrontend melding")

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
                      "norskIdentitetsnummer": "$søkerIdent"
                    },
                    "søknadId": "$søknadId",
                    "versjon": "1.0.0",
                    "ytelse": {
                      "type": "UNGDOMSYTELSE",
                      "søknadType": "DELTAKELSE_SØKNAD",
                      "søktFraDatoer": ["2025-01-01"],
                      "inntekter": {
                        "oppgittePeriodeinntekter": [
                          {
                            "arbeidstakerOgFrilansInntekt": "6000",
                            "næringsinntekt": "0",
                            "ytelse": "2000",
                            "periode": "2025-01-01/2025-01-31"
                          }
                        ]
                      } 
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

        producer.leggPåTopic(søknadId, søknad, TOPIC)

        // Vent til at alle forsøkene er gjort
        await.atMost(Duration.ofSeconds(20)).untilAsserted {
            // 3 feil + 1 suksess = 4 kall totalt
            verify(exactly = 4) {
                ungdomsytelsesøknadService.håndterMottattSøknad(any())
            }

            assertThat(søknadRepository.findAll()).isEmpty()
        }
    }
}
