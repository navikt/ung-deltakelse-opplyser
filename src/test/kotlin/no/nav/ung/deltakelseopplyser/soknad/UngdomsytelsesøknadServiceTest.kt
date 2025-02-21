package no.nav.ung.deltakelseopplyser.soknad

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Kildesystem
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Språk
import no.nav.k9.søknad.ytelse.ung.v1.UngSøknadstype
import no.nav.k9.søknad.ytelse.ung.v1.Ungdomsytelse
import no.nav.pdl.generated.enums.IdentGruppe
import no.nav.pdl.generated.hentident.IdentInformasjon
import no.nav.ung.deltakelseopplyser.deltaker.DeltakerDTO
import no.nav.ung.deltakelseopplyser.deltaker.DeltakerInfoService
import no.nav.ung.deltakelseopplyser.integration.pdl.api.PdlService
import no.nav.ung.deltakelseopplyser.integration.ungsak.UngSakService
import no.nav.ung.deltakelseopplyser.register.DeltakelseOpplysningDTO
import no.nav.ung.deltakelseopplyser.register.UngdomsprogramDeltakelseRepository
import no.nav.ung.deltakelseopplyser.register.UngdomsprogramregisterService
import no.nav.ung.deltakelseopplyser.soknad.kafka.Ungdomsytelsesøknad
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
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
    DeltakerInfoService::class,
    UngdomsytelsesøknadService::class,
    UngdomsprogramregisterService::class,
)
class UngdomsytelsesøknadServiceTest {

    @MockkBean
    lateinit var pdlService: PdlService

    @MockkBean
    lateinit var ungSakService: UngSakService

    @Autowired
    lateinit var ungdomsprogramDeltakelseRepository: UngdomsprogramDeltakelseRepository

    @Autowired
    lateinit var registerService: UngdomsprogramregisterService

    @Autowired
    lateinit var ungdomsytelsesøknadService: UngdomsytelsesøknadService

    @BeforeAll
    fun setUp() {
        ungdomsprogramDeltakelseRepository.deleteAll()
    }

    @Test
    fun `Forventer at søknad markerer deltakelsen som søkt`() {
        val søknadId = "49d5cdb9-13be-450f-8327-187a03bed1a3"
        val søkerIdent = "12345678910"
        val deltakelseStart = "2024-11-04"

        mockPdlIdent(søkerIdent, IdentGruppe.FOLKEREGISTERIDENT)
        val deltakelseOpplysningDTO = meldInnIProgrammet(søkerIdent, deltakelseStart)

        ungdomsytelsesøknadService.håndterMottattSøknad(
            ungdomsytelsesøknad = lagUngdomsytelseSøknad(søknadId, søkerIdent, deltakelseStart)
        )

        val deltakelse = ungdomsprogramDeltakelseRepository.findById(deltakelseOpplysningDTO.id!!)
        assertThat(deltakelse)
            .withFailMessage("Forventet å finne deltakelse med id %s", deltakelseOpplysningDTO.id)
            .isNotEmpty

        assertThat(deltakelse.get().harSøkt)
            .withFailMessage("Forventet at deltakelse med id %s var markert som søkt", deltakelseOpplysningDTO.id)
            .isTrue
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
                harSøkt = false,
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
