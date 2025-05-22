package no.nav.ung.deltakelseopplyser.domene.register

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import jakarta.persistence.EntityManager
import no.nav.pdl.generated.enums.IdentGruppe
import no.nav.pdl.generated.hentident.IdentInformasjon
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.ung.deltakelseopplyser.domene.inntekt.RapportertInntektService
import no.nav.ung.deltakelseopplyser.domene.varsler.MineSiderVarselService
import no.nav.ung.deltakelseopplyser.integration.kontoregister.KontoregisterService
import no.nav.ung.deltakelseopplyser.integration.pdl.api.PdlService
import no.nav.ung.deltakelseopplyser.integration.ungsak.UngSakService
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseOpplysningDTO
import no.nav.ung.deltakelseopplyser.kontrakt.veileder.EndrePeriodeDatoDTO
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.exception.ConstraintViolationException
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.util.*


@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableMockOAuth2Server
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension::class)
class UngdomsprogramregisterServiceTest {

    @Autowired
    lateinit var ungdomsprogramregisterService: UngdomsprogramregisterService

    @Autowired
    lateinit var repository: UngdomsprogramDeltakelseRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @MockkBean
    lateinit var mineSiderVarselService: MineSiderVarselService

    @MockkBean(relaxed = true)
    lateinit var ungSakService: UngSakService

    @MockkBean(relaxed = true)
    lateinit var kontoregisterService: KontoregisterService

    @MockkBean(relaxed = true)
    lateinit var pdlService: PdlService

    @MockkBean
    lateinit var rapportertInntektService: RapportertInntektService

    @BeforeEach
    fun setUp() {
        repository.deleteAll()
        justRun { mineSiderVarselService.opprettVarsel(any(), any(), any(), any(), any(), any()) }
    }

    @AfterAll
    internal fun tearDown() {
        repository.deleteAll()
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(UngdomsprogramregisterServiceTest::class.java)
    }

    @Test
    fun `Deltaker blir meldt inn i programmet uten en sluttdato`() {
        val deltakerDTO = DeltakerDTO(deltakerIdent = "123")
        val dto = DeltakelseOpplysningDTO(
            deltaker = deltakerDTO,
            fraOgMed = LocalDate.now(),
            tilOgMed = null,
            oppgaver = listOf()
        )
        val innmelding = ungdomsprogramregisterService.leggTilIProgram(dto)

        assertNotNull(innmelding)
        assertNotNull(innmelding.id)
        assertNotNull(innmelding.deltaker.id)
    }

    @Test
    fun `Innmelding av deltakelse med overlappende perioder feiler`() {
        val mandag = LocalDate.parse("2024-10-07")
        val onsdag = LocalDate.parse("2024-10-09")

        val deltakerDTO = DeltakerDTO(UUID.randomUUID(), "02499435811")
        val dto = DeltakelseOpplysningDTO(
            deltaker = deltakerDTO,
            fraOgMed = mandag,
            tilOgMed = null,
            oppgaver = listOf()
        )

        every { pdlService.hentFolkeregisteridenter(any()) } returns listOf(
            IdentInformasjon("02499435811", false, IdentGruppe.FOLKEREGISTERIDENT),
            IdentInformasjon("451", true, IdentGruppe.FOLKEREGISTERIDENT)
        )

        ungdomsprogramregisterService.leggTilIProgram(dto)
        entityManager.flush()

        // Skal feile fordi deltaker allerede er meldt inn i programmet uten t.o.m dato.
        assertThrows<ConstraintViolationException> {
            ungdomsprogramregisterService.leggTilIProgram(dto.copy(fraOgMed = onsdag))
            entityManager.flush()
        }
    }


    @Test
    fun `Deltaker blir meldt inn i programmet med en sluttdato`() {
        val deltakerDTO = DeltakerDTO(deltakerIdent = "123")
        val dto = DeltakelseOpplysningDTO(
            deltaker = deltakerDTO,
            fraOgMed = LocalDate.now(),
            tilOgMed = LocalDate.now().plusDays(10),
            oppgaver = listOf()
        )
        val innmelding = ungdomsprogramregisterService.leggTilIProgram(dto)

        assertNotNull(innmelding)
        assertNotNull(innmelding.id)
        assertNotNull(innmelding.deltaker.id)
    }

    @Test
    fun `Deltaker blir fjernet fra programmet`() {
        val deltakerDTO = DeltakerDTO(UUID.randomUUID(), "123")
        val dto = DeltakelseOpplysningDTO(
            deltaker = deltakerDTO,
            fraOgMed = LocalDate.now(),
            tilOgMed = null,
            oppgaver = listOf()
        )
        val innmelding = ungdomsprogramregisterService.leggTilIProgram(dto)

        val utmelding = ungdomsprogramregisterService.fjernFraProgram(innmelding.id!!)

        assertTrue(utmelding)
    }

    @Test
    fun `Henter deltaker fra programmet`() {
        val deltakerDTO = DeltakerDTO(deltakerIdent = "123")
        val dto = DeltakelseOpplysningDTO(
            deltaker = deltakerDTO,
            fraOgMed = LocalDate.now(),
            tilOgMed = null,
            oppgaver = listOf()
        )
        val innmelding = ungdomsprogramregisterService.leggTilIProgram(dto)

        val hentetDto = ungdomsprogramregisterService.hentFraProgram(innmelding.id!!)

        assertNotNull(hentetDto)
        assertNotNull(hentetDto.deltaker.id)
    }

    @Test
    fun `Endring av startdato på deltakelse oppretter BEKREFT_ENDRET_STARTDATO oppgave`() {
        every { pdlService.hentAktørIder(any(), true) } returns listOf(
            IdentInformasjon("321", false, IdentGruppe.AKTORID),
            IdentInformasjon("451", true, IdentGruppe.AKTORID)
        )

        val mandag = LocalDate.parse("2024-10-07")
        val onsdag = LocalDate.parse("2024-10-09")

        val deltakerDTO = DeltakerDTO(deltakerIdent = "123")
        val dto = DeltakelseOpplysningDTO(
            deltaker = deltakerDTO,
            fraOgMed = mandag,
            tilOgMed = null,
            oppgaver = listOf()
        )
        val innmelding = ungdomsprogramregisterService.leggTilIProgram(dto)

        val endretStartdatoDeltakelse =
            ungdomsprogramregisterService.endreStartdato(innmelding.id!!, mockEndrePeriodeDTO(onsdag))

        assertNotNull(endretStartdatoDeltakelse)
        assertEquals(innmelding.deltaker, endretStartdatoDeltakelse.deltaker)
        assertThat(endretStartdatoDeltakelse.fraOgMed).isEqualTo(onsdag)
        assertThat(endretStartdatoDeltakelse.tilOgMed).isNull()
    }

    @Test
    fun `Endring av sluttdato på deltakelse oppretter BEKREFT_ENDRET_SLUTTDATODATO oppgave`() {
        val mandag = LocalDate.parse("2024-10-07")
        val onsdag = LocalDate.parse("2024-10-09")

        val deltakerDTO = DeltakerDTO(deltakerIdent = "123")
        val dto = DeltakelseOpplysningDTO(
            deltaker = deltakerDTO,
            fraOgMed = mandag,
            tilOgMed = null,
            oppgaver = listOf()
        )
        val innmelding = ungdomsprogramregisterService.leggTilIProgram(dto)

        every { pdlService.hentAktørIder(any(), true) } returns listOf(
            IdentInformasjon("321", false, IdentGruppe.AKTORID),
            IdentInformasjon("451", true, IdentGruppe.AKTORID)
        )

        val oppdatertDto = DeltakelseOpplysningDTO(
            deltaker = innmelding.deltaker,
            fraOgMed = mandag,
            tilOgMed = onsdag,
            oppgaver = listOf()
        )
        ungdomsprogramregisterService.avsluttDeltakelse(innmelding.id!!, oppdatertDto)

        val endretSluttdatoDeltakelse =
            ungdomsprogramregisterService.endreSluttdato(innmelding.id!!, mockEndrePeriodeDTO(onsdag.plusWeeks(1)))

        assertNotNull(endretSluttdatoDeltakelse)
        assertEquals(innmelding.deltaker, endretSluttdatoDeltakelse.deltaker)
        assertThat(endretSluttdatoDeltakelse.fraOgMed).isEqualTo(mandag)
        assertThat(endretSluttdatoDeltakelse.tilOgMed).isEqualTo(onsdag.plusWeeks(1))

        val historikk = ungdomsprogramregisterService.historikk(innmelding.id!!)
        assertThat(historikk).isNotEmpty
        historikk.forEach {
            logger.info("Innslag: {}", it)
        }
    }

    private fun mockEndrePeriodeDTO(dato: LocalDate) = EndrePeriodeDatoDTO(dato = dato)
}
