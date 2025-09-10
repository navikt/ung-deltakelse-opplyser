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
import no.nav.ung.deltakelseopplyser.domene.deltaker.Scenarioer
import no.nav.ung.deltakelseopplyser.domene.inntekt.RapportertInntektService
import no.nav.ung.deltakelseopplyser.domene.minside.MineSiderService
import no.nav.ung.deltakelseopplyser.domene.minside.mikrofrontend.MicrofrontendRepository
import no.nav.ung.deltakelseopplyser.domene.minside.mikrofrontend.MicrofrontendService
import no.nav.ung.deltakelseopplyser.domene.minside.mikrofrontend.MicrofrontendStatus
import no.nav.ung.deltakelseopplyser.domene.oppgave.OppgaveMapperService
import no.nav.ung.deltakelseopplyser.domene.oppgave.OppgaveService
import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseRepository
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService
import no.nav.ung.deltakelseopplyser.domene.soknad.kafka.Ungdomsytelsesøknad
import no.nav.ung.deltakelseopplyser.integration.abac.SifAbacPdpService
import no.nav.ung.deltakelseopplyser.integration.enhetsregisteret.EnhetsregisterService
import no.nav.ung.deltakelseopplyser.integration.kontoregister.KontoregisterService
import no.nav.ung.deltakelseopplyser.integration.pdl.api.PdlService
import no.nav.ung.deltakelseopplyser.integration.ungsak.UngSakService
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseDTO
import no.nav.ung.deltakelseopplyser.utils.FødselsnummerGenerator
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
import java.util.*

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
    OppgaveMapperService::class
)
class UngdomsytelsesøknadServiceTest {

    @MockkBean
    lateinit var pdlService: PdlService


    @MockkBean(relaxed = true)
    lateinit var sifAbacPdpService: SifAbacPdpService

    @MockkBean
    lateinit var ungSakService: UngSakService

    @MockkBean
    lateinit var kontoregisterService: KontoregisterService

    @MockkBean
    lateinit var mineSiderService: MineSiderService

    @MockkBean
    lateinit var enhetsregisterService: EnhetsregisterService

    @Autowired
    lateinit var deltakelseRepository: DeltakelseRepository

    @Autowired
    lateinit var registerService: UngdomsprogramregisterService

    @Autowired
    lateinit var ungdomsytelsesøknadService: UngdomsytelsesøknadService

    @Autowired
    lateinit var microfrontendRepository: MicrofrontendRepository

    @Autowired
    lateinit var deltakerService: DeltakerService

    @BeforeAll
    fun setUp() {
        justRun { mineSiderService.opprettVarsel(any(), any(), any(), any(), any(), any()) }
        justRun { mineSiderService.aktiverMikrofrontend(any(), any(), any()) }
        justRun { mineSiderService.deaktiverOppgave(any()) }
    }

    @Test
    fun `Forventer at søknad markerer deltakelsen som søkt og oppgaven løses`() {
        val søkerIdent = FødselsnummerGenerator.neste()
        val deltakelseStart = "2024-11-04"

        mockPdlIdent(søkerIdent, IdentGruppe.FOLKEREGISTERIDENT)
        val deltakelseDTO = meldInnIProgrammet(søkerIdent, deltakelseStart)
        val sendSøknadOppgave = deltakerService.hentDeltakersOppgaver(søkerIdent).find { it.oppgavetype == Oppgavetype.SØK_YTELSE }
            ?: throw IllegalStateException("Fant ikke send søknad oppgave for deltaker med ident $søkerIdent")

        ungdomsytelsesøknadService.håndterMottattSøknad(
            ungdomsytelsesøknad = lagUngdomsytelseSøknad(
                søknadId = sendSøknadOppgave.oppgaveReferanse.toString(),
                deltakelseId = deltakelseDTO.id!!,
                søkerIdent = søkerIdent,
                deltakelseStart = deltakelseStart
            )
        )

        val deltakelse = deltakelseRepository.findById(deltakelseDTO.id!!)
        assertThat(deltakelse)
            .withFailMessage("Forventet å finne deltakelse med id %s", deltakelseDTO.id)
            .isNotEmpty

        assertThat(deltakelse.get().søktTidspunkt)
            .withFailMessage("Forventet at deltakelse med id %s var markert som søkt", deltakelseDTO.id)
            .isNotNull()


        val deltakerDAO = deltakelse.get().deltaker
        val microfrontendStatusDAO = microfrontendRepository.findByDeltaker(deltakerDAO)
        assertThat(microfrontendStatusDAO).isNotNull
            .withFailMessage("Forventet å finne mikrofrontend for deltaker med id %s", deltakerDAO.id)

        assertThat(microfrontendStatusDAO!!)
            .withFailMessage(
                "Forventet at mikrofrontend ble aktivert for deltaker med id %s",
                deltakerDAO.id
            )
            .matches { it.status == MicrofrontendStatus.ENABLE }
    }

    private fun lagUngdomsytelseSøknad(
        søknadId: String,
        deltakelseId: UUID,
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
                    .medDeltakelseId(deltakelseId)
            )
    )

    private fun meldInnIProgrammet(søkerIdent: String, deltakelseStart: String): DeltakelseDTO {
        return registerService.leggTilIProgram(
            deltakelseDTO = DeltakelseDTO(
                deltaker = DeltakerDTO(deltakerIdent = søkerIdent),
                fraOgMed = LocalDate.parse(deltakelseStart),
                tilOgMed = null
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
}
