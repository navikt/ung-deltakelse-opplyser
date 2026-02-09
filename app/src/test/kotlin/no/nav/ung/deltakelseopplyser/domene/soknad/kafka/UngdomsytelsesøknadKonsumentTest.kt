package no.nav.ung.deltakelseopplyser.domene.soknad.kafka

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.mockk.andThenJust
import io.mockk.every
import io.mockk.runs
import io.mockk.verify
import no.nav.pdl.generated.enums.IdentGruppe
import no.nav.pdl.generated.hentident.IdentInformasjon
import no.nav.ung.deltakelseopplyser.AbstractIntegrationTest
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.deltaker.Scenarioer
import no.nav.ung.deltakelseopplyser.domene.minside.mikrofrontend.MicrofrontendService
import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseRepository
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService
import no.nav.ung.deltakelseopplyser.domene.soknad.UngdomsytelsesøknadService
import no.nav.ung.deltakelseopplyser.domene.soknad.repository.SøknadRepository
import no.nav.ung.deltakelseopplyser.integration.abac.SifAbacPdpService
import no.nav.ung.deltakelseopplyser.integration.pdl.api.PdlService
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveStatus
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseDTO
import no.nav.ung.deltakelseopplyser.utils.FødselsnummerGenerator
import no.nav.ung.deltakelseopplyser.utils.KafkaUtils.leggPåTopic
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.time.LocalDate

class UngdomsytelsesøknadKonsumentTest : AbstractIntegrationTest() {

    private companion object {
        const val TOPIC = "dusseldorf.ungdomsytelse-soknad-cleanup"
    }

    override val consumerGroupPrefix: String = "ungdomsytelsesoknad-konsument"
    override val consumerGroupTopics: List<String> = listOf("dusseldorf.ungdomsytelse-soknad-cleanup")

    @Autowired
    lateinit var søknadRepository: SøknadRepository

    @Autowired
    lateinit var ungdomsprogramregisterService: UngdomsprogramregisterService

    @Autowired
    lateinit var deltakerService: DeltakerService

    @Autowired
    lateinit var deltakelseRepository: DeltakelseRepository

    @SpykBean
    lateinit var ungdomsytelsesøknadService: UngdomsytelsesøknadService

    @MockkBean
    lateinit var microfrontendService: MicrofrontendService

    @MockkBean
    lateinit var pdlService: PdlService

    @MockkBean(relaxed = true)
    lateinit var sifAbacPdpService: SifAbacPdpService

    @Test
    fun `Forventer at listener forsøker på nytt ved feil`() {
        val deltakerIdent = FødselsnummerGenerator.neste()
        mockPdl(deltakerIdent, IdentGruppe.FOLKEREGISTERIDENT)

        val journalpostId = "671161658"

        val deltakelseDTO = ungdomsprogramregisterService.leggTilIProgram(
            DeltakelseDTO(
                deltaker = DeltakerDTO(deltakerIdent = deltakerIdent),
                fraOgMed = LocalDate.now(),
                tilOgMed = null
            )
        )

        val oppgaveReferanse = deltakerService.hentDeltakersOppgaver(deltakerIdent).first { it.oppgavetype == Oppgavetype.SØK_YTELSE }.oppgaveReferanse

        every { microfrontendService.sendOgLagre(any()) } throwsMany listOf(
                RuntimeException("Simulert feil 1"),
                RuntimeException("Simulert feil 2"),
                RuntimeException("Simulert feil 3")
            ) andThenJust runs

        //language=JSON
        val søknad = """
            {
              "data": {
                "journalførtMelding": {
                  "type": "JournalfortSøknad",
                  "journalpostId": "$journalpostId",
                  "søknad": {
                    "mottattDato": "2024-11-04T10:57:18.634Z",
                    "språk": "nb",
                    "søker": {
                      "norskIdentitetsnummer": "$deltakerIdent"
                    },
                    "søknadId": "$oppgaveReferanse",
                    "versjon": "1.0.0",
                    "ytelse": {
                      "type": "UNGDOMSYTELSE",
                      "deltakelseId": "${deltakelseDTO.id}",
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
                "correlationId": "cd9b224f-b344-480c-8513-f68a19cb7b3a",
                "soknadDialogCommitSha": "2024.11.04-09.27-1d1c461",
                "version": 1
              }
            }
        """.trimIndent()

        producer.leggPåTopic(oppgaveReferanse.toString(), søknad, TOPIC)

        // Vent til at alle forsøkene er gjort
        await.atMost(Duration.ofSeconds(60)).untilAsserted {
            // 3 feil + 1 suksess = 4 kall totalt
            verify(atLeast = 4) {
                ungdomsytelsesøknadService.håndterMottattSøknad(any())
            }

            deltakerService.hentDeltakersOppgaver(deltakerIdent).first { it.oppgaveReferanse == oppgaveReferanse }.let { oppgave ->
                assertThat(oppgave.status).isEqualTo(OppgaveStatus.LØST)
            }

            deltakelseRepository.findById(deltakelseDTO.id!!).get().let { deltakelse ->
                assertThat(deltakelse.søktTidspunkt).isNotNull
            }

            assertThat(søknadRepository.findById(journalpostId)). isPresent
        }
    }

    private fun mockPdl(deltakerIdent: String, identGruppe: IdentGruppe) {
        val pdlPerson = IdentInformasjon(
            ident = deltakerIdent,
            historisk = false,
            gruppe = identGruppe
        )

        every { pdlService.hentFolkeregisteridenter(any()) } returns listOf(pdlPerson)
        every { pdlService.hentPerson(any()) } returns Scenarioer
            .lagPerson(LocalDate.of(2000, 1, 1))
        every { pdlService.hentAktørIder(any()) } returns listOf(
            IdentInformasjon(
                "123456789",
                false,
                IdentGruppe.AKTORID
            ))
    }
}
