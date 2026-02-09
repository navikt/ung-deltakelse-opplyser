package no.nav.ung.deltakelseopplyser.domene.soknad

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import io.mockk.verify
import no.nav.pdl.generated.enums.IdentGruppe
import no.nav.pdl.generated.hentident.IdentInformasjon
import no.nav.ung.deltakelseopplyser.AbstractIntegrationTest
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.deltaker.Scenarioer
import no.nav.ung.deltakelseopplyser.integration.pdl.api.PdlService
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseDTO
import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseRepository
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService
import no.nav.ung.deltakelseopplyser.domene.soknad.repository.SøknadRepository
import no.nav.ung.deltakelseopplyser.integration.abac.SifAbacPdpService
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
import no.nav.ung.deltakelseopplyser.utils.FødselsnummerGenerator
import no.nav.ung.deltakelseopplyser.utils.KafkaUtils.leggPåTopic
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class UngdomsytelseOppgavebekreftelseKonsumentTest : AbstractIntegrationTest() {

    companion object {
        const val TOPIC = "dusseldorf.ungdomsytelse-soknad-cleanup"
    }

    override val consumerGroupPrefix: String = "ungdomsytelsesøknad-konsument"
    override val consumerGroupTopics: List<String> = listOf(TOPIC)

    @SpykBean
    lateinit var ungdomsytelsesøknadService: UngdomsytelsesøknadService

    @SpykBean
    lateinit var deltakerService: DeltakerService

    @SpykBean
    lateinit var deltakelseRepository: DeltakelseRepository

    @SpykBean
    lateinit var søknadRepository: SøknadRepository

    @MockkBean
    lateinit var pdlService: PdlService


    @MockkBean(relaxed = true)
    lateinit var sifAbacPdpService: SifAbacPdpService

    @Autowired
    lateinit var registerService: UngdomsprogramregisterService


    @Test
    fun `Forventet søknad konsumeres og deserialiseres som forventet`() {
        val correlationId = "cd9b224f-b344-480c-8513-f68a19cb7b3a"
        val søkerIdent = FødselsnummerGenerator.neste()
        val deltakelseStart = "2024-11-04"

        mockPdlIdent(søkerIdent, IdentGruppe.FOLKEREGISTERIDENT)
        val deltakelse = meldInnIProgrammet(søkerIdent, deltakelseStart)
        val sendSøknadOppgave = deltakerService.hentDeltakersOppgaver(søkerIdent).find { it.oppgavetype == Oppgavetype.SØK_YTELSE }
            ?: throw IllegalStateException("Fant ikke send søknad oppgave for deltaker med ident $søkerIdent")

        val søknadId = sendSøknadOppgave.oppgaveReferanse.toString()

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
                      "deltakelseId": "${deltakelse.id}",
                      "søktFraDatoer": ["$deltakelseStart"],
                      "inntekter": null
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
            verify(exactly = 1) { ungdomsytelsesøknadService.håndterMottattSøknad(any()) }
            verify(exactly = 1) { deltakerService.hentDeltakterIder(any()) }
            verify(exactly = 1) { deltakelseRepository.findByIdAndDeltaker_IdIn(any(), any()) }
            verify(exactly = 1) { søknadRepository.save(any()) }
        }
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
        every { pdlService.hentAktørIder(any()) } returns listOf(IdentInformasjon("123456789", false, IdentGruppe.AKTORID))

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
