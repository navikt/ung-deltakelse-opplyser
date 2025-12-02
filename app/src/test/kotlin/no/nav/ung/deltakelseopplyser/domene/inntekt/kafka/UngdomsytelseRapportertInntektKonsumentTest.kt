package no.nav.ung.deltakelseopplyser.domene.inntekt.kafka

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import no.nav.pdl.generated.enums.IdentGruppe
import no.nav.pdl.generated.hentident.IdentInformasjon
import no.nav.ung.deltakelseopplyser.AbstractIntegrationTest
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.deltaker.Scenarioer
import no.nav.ung.deltakelseopplyser.domene.inntekt.RapportertInntektHåndtererService
import no.nav.ung.deltakelseopplyser.domene.inntekt.repository.RapportertInntektRepository
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseDTO
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService
import no.nav.ung.deltakelseopplyser.domene.register.ungsak.OppgaveUngSakController
import no.nav.ung.deltakelseopplyser.domene.minside.MineSiderService
import no.nav.ung.deltakelseopplyser.integration.abac.SifAbacPdpService
import no.nav.ung.deltakelseopplyser.integration.abac.TilgangskontrollService
import no.nav.ung.deltakelseopplyser.integration.pdl.api.PdlService
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveStatus
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.inntektsrapportering.InntektsrapporteringOppgaveDTO
import no.nav.ung.deltakelseopplyser.utils.KafkaUtils.leggPåTopic
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit

class UngdomsytelseRapportertInntektKonsumentTest : AbstractIntegrationTest() {

    private companion object {
        const val TOPIC = "dusseldorf.ungdomsytelse-inntektsrapportering-cleanup"
    }

    override val consumerGroupPrefix: String = "ungdomsytelse-inntektsrapportering-konsument"
    override val consumerGroupTopics: List<String> = listOf(TOPIC)

    @SpykBean
    lateinit var rapportertInntektHåndtererService: RapportertInntektHåndtererService

    @SpykBean
    lateinit var deltakerService: DeltakerService

    @SpykBean
    lateinit var rapportertInntektRepository: RapportertInntektRepository

    @SpykBean
    lateinit var mineSiderService: MineSiderService

    @MockkBean
    lateinit var pdlService: PdlService

    @MockkBean(relaxed = true)
    lateinit var sifAbacPdpService: SifAbacPdpService

    @Autowired
    lateinit var registerService: UngdomsprogramregisterService

    @Autowired
    lateinit var oppgaveUngSakController: OppgaveUngSakController

    @MockkBean
    lateinit var tilgangskontrollService: TilgangskontrollService

    @Test
    fun `Forventet rapportertInntekt konsumeres og deserialiseres som forventet`() {
        justRun { tilgangskontrollService.krevSystemtilgang() }
        every { pdlService.hentPerson(any()) } returns Scenarioer
            .lagPerson(LocalDate.of(2000, 1, 1))

        val søknadId = "49d5cdb9-13be-450f-8327-187a03bed1a3"
        val correlationId = "cd9b224f-b344-480c-8513-f68a19cb7b3a"
        val søkerIdent = no.nav.ung.deltakelseopplyser.utils.FødselsnummerGenerator.neste()
        val deltakelseStart = "2024-11-04"

        mockPdlIdent(søkerIdent, IdentGruppe.FOLKEREGISTERIDENT)
        meldInnIProgrammet(søkerIdent, deltakelseStart)

        opprettOppgaveForInntektsrapportering(søkerIdent, søknadId)

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
                      "søknadType": "RAPPORTERING_SØKNAD",
                      "søktFraDatoer": [],
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

        await.atMost(10, TimeUnit.SECONDS).untilAsserted {
            verify(exactly = 1) { rapportertInntektHåndtererService.håndterRapportertInntekt(any()) }
            verify(exactly = 1) { deltakerService.hentDeltakterIder(any()) }
            verify(exactly = 1) { mineSiderService.deaktiverOppgave(any()) }
            verify(exactly = 1) { rapportertInntektRepository.save(any()) }

            val oppgave = deltakerService.hentDeltakersOppgaver(søkerIdent).first()
            assertThat(oppgave.oppgaveReferanse).isEqualTo(UUID.fromString(søknadId))
            assertThat(oppgave.oppgavetype).isEqualTo(Oppgavetype.RAPPORTER_INNTEKT)
            assertThat(oppgave.status).isEqualTo(OppgaveStatus.LØST)
        }
    }

    private fun opprettOppgaveForInntektsrapportering(deltakelseStart: String, søknadId: String) {
        val now = LocalDate.now()
        oppgaveUngSakController.opprettOppgaveForInntektsrapportering(
            opprettInntektsrapporteringOppgaveDTO = InntektsrapporteringOppgaveDTO(
                deltakerIdent = deltakelseStart,
                referanse = UUID.fromString(søknadId),
                frist = LocalDateTime.now().plusDays(6),
                fomDato = now.withDayOfMonth(1),
                tomDato = now.withDayOfMonth(now.lengthOfMonth()),
                gjelderDelerAvMåned = false
            )
        )
    }

    private fun mockPdlIdent(søkerIdent: String, identGruppe: IdentGruppe) {
        val pdlPerson = IdentInformasjon(
            ident = søkerIdent,
            historisk = false,
            gruppe = identGruppe
        )

        every { pdlService.hentFolkeregisteridenter(any()) } returns listOf(pdlPerson)
        every { pdlService.hentPerson(any()) } returns Scenarioer
            .lagPerson(LocalDate.of(2000, 1, 1))

    }

    private fun meldInnIProgrammet(søkerIdent: String, deltakelseStart: String): DeltakelseDTO {
        return registerService.leggTilIProgram(
            deltakelseDTO = DeltakelseDTO(
                deltaker = DeltakerDTO(deltakerIdent = søkerIdent),
                fraOgMed = LocalDate.parse(deltakelseStart),
                tilOgMed = null
            )
        )
    }
}
