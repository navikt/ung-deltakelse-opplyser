package no.nav.ung.deltakelseopplyser.domene.soknad

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Kildesystem
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Språk
import no.nav.k9.søknad.ytelse.ung.v1.UngSøknadstype
import no.nav.k9.søknad.ytelse.ung.v1.Ungdomsytelse
import no.nav.pdl.generated.enums.IdentGruppe
import no.nav.pdl.generated.hentident.IdentInformasjon
import no.nav.ung.deltakelseopplyser.config.DeltakerappConfig
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.inntekt.RapportertInntektService
import no.nav.ung.deltakelseopplyser.domene.minside.MineSiderService
import no.nav.ung.deltakelseopplyser.domene.minside.mikrofrontend.MicrofrontendRepository
import no.nav.ung.deltakelseopplyser.domene.minside.mikrofrontend.MicrofrontendService
import no.nav.ung.deltakelseopplyser.domene.minside.mikrofrontend.MicrofrontendStatus
import no.nav.ung.deltakelseopplyser.domene.oppgave.OppgaveService
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramDeltakelseRepository
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService
import no.nav.ung.deltakelseopplyser.domene.soknad.kafka.Ungdomsytelsesøknad
import no.nav.ung.deltakelseopplyser.integration.kontoregister.KontoregisterService
import no.nav.ung.deltakelseopplyser.integration.pdl.api.PdlService
import no.nav.ung.deltakelseopplyser.integration.ungsak.UngSakService
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseOpplysningDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.time.ZonedDateTime

@DataJpaTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension::class)
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE
)
@Import(
    DeltakerService::class,
    UngdomsytelsesøknadService::class,
    UngdomsprogramregisterService::class,
    RapportertInntektService::class,
    DeltakerappConfig::class,
    MicrofrontendService::class,
    OppgaveService::class,
)
class UngdomsytelsesøknadServiceTest {

    @MockkBean
    lateinit var pdlService: PdlService

    @MockkBean
    lateinit var ungSakService: UngSakService

    @MockkBean
    lateinit var kontoregisterService: KontoregisterService

    @MockkBean
    lateinit var mineSiderService: MineSiderService

    @Autowired
    lateinit var ungdomsprogramDeltakelseRepository: UngdomsprogramDeltakelseRepository

    @Autowired
    lateinit var registerService: UngdomsprogramregisterService

    @Autowired
    lateinit var ungdomsytelsesøknadService: UngdomsytelsesøknadService

    @Autowired
    lateinit var microfrontendRepository: MicrofrontendRepository

    @BeforeAll
    fun setUp() {
        ungdomsprogramDeltakelseRepository.deleteAll()
        microfrontendRepository.deleteAll()
        justRun { mineSiderService.opprettVarsel(any(), any(), any(), any(), any(), any()) }
        justRun { mineSiderService.aktiverMikrofrontend(any(), any(), any()) }
        justRun { mineSiderService.deaktiverOppgave(any()) }
    }

    @Test
    fun `Forventer at søknad markerer deltakelsen som søkt og oppgaven løses`() {
        val søkerIdent = "12345678910"
        val deltakelseStart = "2024-11-04"

        mockPdlIdent(søkerIdent, IdentGruppe.FOLKEREGISTERIDENT)
        val deltakelseOpplysningDTO = meldInnIProgrammet(søkerIdent, deltakelseStart)
        val sendSøknadOppgave = deltakelseOpplysningDTO.oppgaver.find { it.oppgavetype == Oppgavetype.SØK_YTELSE }
            ?: throw IllegalStateException("Fant ikke send søknad oppgave for deltaker med ident $søkerIdent")

        ungdomsytelsesøknadService.håndterMottattSøknad(
            ungdomsytelsesøknad = lagUngdomsytelseSøknad(
                søknadId = sendSøknadOppgave.oppgaveReferanse.toString(),
                søkerIdent = søkerIdent,
                deltakelseStart = deltakelseStart
            )
        )

        val deltakelse = ungdomsprogramDeltakelseRepository.findById(deltakelseOpplysningDTO.id!!)
        assertThat(deltakelse)
            .withFailMessage("Forventet å finne deltakelse med id %s", deltakelseOpplysningDTO.id)
            .isNotEmpty

        assertThat(deltakelse.get().søktTidspunkt)
            .withFailMessage("Forventet at deltakelse med id %s var markert som søkt", deltakelseOpplysningDTO.id)
            .isNotNull()

        assertThat(microfrontendRepository.findAll())
            .withFailMessage(
                "Forventet at mikrofrontend ble aktivert for deltakelse med id %s",
                deltakelseOpplysningDTO.id
            )
            .isNotEmpty
            .hasSize(1)
            .first()
            .matches { it.deltaker.deltakerIdent == søkerIdent }
            .matches { it.status == MicrofrontendStatus.ENABLE }
    }

    private fun lagUngdomsytelseSøknad(
        søknadId: String,
        søkerIdent: String,
        deltakelseStart: String,
    ) = Ungdomsytelsesøknad(
        journalpostId = "671161658",
        søknad = Søknad()
            .medSøknadId(søknadId)
            .medMottattDato(ZonedDateTime.now())
            .medSpråk(Språk.NORSK_BOKMÅL)
            .medKildesystem(Kildesystem.SØKNADSDIALOG)
            .medSøker(Søker(NorskIdentitetsnummer.of(søkerIdent)))
            .medYtelse(
                Ungdomsytelse()
                    .medSøknadType(UngSøknadstype.DELTAKELSE_SØKNAD)
                    .medStartdato(LocalDate.parse(deltakelseStart))
            )
    )

    private fun meldInnIProgrammet(søkerIdent: String, deltakelseStart: String): DeltakelseOpplysningDTO {
        return registerService.leggTilIProgram(
            deltakelseOpplysningDTO = DeltakelseOpplysningDTO(
                deltaker = DeltakerDTO(deltakerIdent = søkerIdent),
                fraOgMed = LocalDate.parse(deltakelseStart),
                tilOgMed = null,
                oppgaver = listOf()
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
    }
}
