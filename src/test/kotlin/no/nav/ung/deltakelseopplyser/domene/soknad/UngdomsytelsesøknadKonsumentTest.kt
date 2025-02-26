package no.nav.ung.deltakelseopplyser.domene.soknad

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import io.mockk.verify
import no.nav.pdl.generated.enums.IdentGruppe
import no.nav.pdl.generated.hentident.IdentInformasjon
import no.nav.ung.deltakelseopplyser.AbstractIntegrationTest
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDTO
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.soknad.UngdomsytelsesøknadService
import no.nav.ung.deltakelseopplyser.integration.pdl.api.PdlService
import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseOpplysningDTO
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramDeltakelseRepository
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService
import no.nav.ung.deltakelseopplyser.domene.soknad.repository.SøknadRepository
import no.nav.ung.deltakelseopplyser.utils.KafkaUtils.leggPåTopic
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class UngdomsytelsesøknadKonsumentTest : AbstractIntegrationTest() {

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
    lateinit var ungdomsprogramDeltakelseRepository: UngdomsprogramDeltakelseRepository

    @SpykBean
    lateinit var søknadRepository: SøknadRepository

    @MockkBean
    lateinit var pdlService: PdlService

    @Autowired
    lateinit var registerService: UngdomsprogramregisterService

    @Test
    fun `Forventet søknad konsumeres og deserialiseres som forventet`() {
        val søknadId = "49d5cdb9-13be-450f-8327-187a03bed1a3"
        val correlationId = "cd9b224f-b344-480c-8513-f68a19cb7b3a"
        val søkerIdent = "12345678910"
        val deltakelseStart = "2024-11-04"

        mockPdlIdent(søkerIdent, IdentGruppe.FOLKEREGISTERIDENT)
        meldInnIProgrammet(søkerIdent, deltakelseStart)

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
            verify(exactly = 1) { ungdomsprogramDeltakelseRepository.finnDeltakelseSomStarter(any(), any()) }
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
    }

    private fun meldInnIProgrammet(søkerIdent: String, deltakelseStart: String): DeltakelseOpplysningDTO {
        return registerService.leggTilIProgram(
            deltakelseOpplysningDTO = DeltakelseOpplysningDTO(
                deltaker = DeltakerDTO(deltakerIdent = søkerIdent),
                harSøkt = false,
                fraOgMed = LocalDate.parse(deltakelseStart),
                tilOgMed = null,
                oppgaver = listOf()
            )
        )
    }
}
