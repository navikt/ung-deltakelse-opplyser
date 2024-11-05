package no.nav.ung.deltakelseopplyser.register

import com.ninjasquad.springmockk.MockkBean
import io.hypersistence.utils.hibernate.type.range.Range
import io.mockk.every
import io.mockk.verify
import jakarta.persistence.EntityManager
import no.nav.pdl.generated.enums.IdentGruppe
import no.nav.pdl.generated.hentident.IdentInformasjon
import no.nav.ung.deltakelseopplyser.integration.k9sak.K9SakService
import no.nav.ung.deltakelseopplyser.integration.pdl.PdlService
import no.nav.ung.deltakelseopplyser.register.UngdomsprogramregisterService.Companion.somDeltakelsePeriodInfo
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
@Import(UngdomsprogramregisterService::class)
class UngdomsprogramregisterServiceTest {

    @Autowired
    lateinit var ungdomsprogramregisterService: UngdomsprogramregisterService

    @Autowired
    lateinit var repository: UngdomsprogramDeltakelseRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @MockkBean(relaxed = true)
    lateinit var k9SakService: K9SakService

    @MockkBean(relaxed = true)
    lateinit var pdlService: PdlService

    @BeforeEach
    fun setUp() {
        repository.deleteAll()
    }

    @AfterAll
    internal fun tearDown() {
        repository.deleteAll()
    }

    @Test
    fun `Deltaker blir meldt inn i programmet uten en sluttdato`() {
        val dto = DeltakelseOpplysningDTO(
            deltakerIdent = "123",
            fraOgMed = LocalDate.now(),
            harSøkt = false,
            tilOgMed = null
        )
        val innmelding = ungdomsprogramregisterService.leggTilIProgram(dto)

        assertNotNull(innmelding)
        assertNotNull(innmelding.id)
        assertEquals(dto.deltakerIdent, innmelding.deltakerIdent)
    }

    @Test
    fun `Innmelding av deltakelse med overlappende perioder feiler`() {
        val mandag = LocalDate.parse("2024-10-07")
        val onsdag = LocalDate.parse("2024-10-09")

        val dto = DeltakelseOpplysningDTO(
            deltakerIdent = "02499435811",
            fraOgMed = mandag,
            harSøkt = false,
            tilOgMed = null
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
        val dto = DeltakelseOpplysningDTO(
            deltakerIdent = "123",
            harSøkt = false,
            fraOgMed = LocalDate.now(),
            tilOgMed = LocalDate.now().plusDays(10)
        )
        val innmelding = ungdomsprogramregisterService.leggTilIProgram(dto)

        assertNotNull(innmelding)
        assertNotNull(innmelding.id)
        assertEquals(dto.deltakerIdent, innmelding.deltakerIdent)
    }

    @Test
    fun `Deltaker blir fjernet fra programmet`() {
        val dto = DeltakelseOpplysningDTO(
            deltakerIdent = "123",
            harSøkt = false,
            fraOgMed = LocalDate.now(),
            tilOgMed = null
        )
        val innmelding = ungdomsprogramregisterService.leggTilIProgram(dto)

        val utmelding = ungdomsprogramregisterService.fjernFraProgram(innmelding.id!!)

        assertTrue(utmelding)
    }

    @Test
    fun `Deltaker blir oppdatert i programmet`() {
        val mandag = LocalDate.parse("2024-10-07")
        val onsdag = LocalDate.parse("2024-10-09")

        val dto = DeltakelseOpplysningDTO(
            deltakerIdent = "123",
            fraOgMed = mandag,
            harSøkt = false,
            tilOgMed = null
        )
        val innmelding = ungdomsprogramregisterService.leggTilIProgram(dto)


        every { pdlService.hentAktørIder(any(), true) } returns listOf(
            IdentInformasjon("321", false, IdentGruppe.AKTORID),
            IdentInformasjon("451", true, IdentGruppe.AKTORID)
        )

        val oppdatertDto = DeltakelseOpplysningDTO(
            deltakerIdent = "123",
            fraOgMed = mandag,
            harSøkt = false,
            tilOgMed = onsdag
        )
        val oppdatertInnmelding = ungdomsprogramregisterService.oppdaterProgram(innmelding.id!!, oppdatertDto)

        assertNotNull(oppdatertInnmelding)
        assertEquals(oppdatertDto.deltakerIdent, oppdatertInnmelding.deltakerIdent)
        assertEquals(oppdatertDto.tilOgMed, oppdatertInnmelding.tilOgMed)

        verify { pdlService.hentAktørIder(any(), any()) }
        verify { k9SakService.sendInnHendelse(any()) }
    }

    @Test
    fun `Henter deltaker fra programmet`() {
        val dto = DeltakelseOpplysningDTO(
            deltakerIdent = "123",
            fraOgMed = LocalDate.now(),
            harSøkt = false,
            tilOgMed = null
        )
        val innmelding = ungdomsprogramregisterService.leggTilIProgram(dto)

        val hentetDto = ungdomsprogramregisterService.hentFraProgram(innmelding.id!!)

        assertNotNull(hentetDto)
        assertEquals(dto.deltakerIdent, hentetDto.deltakerIdent)
    }

    @Test
    fun `Deltakelsesperioder med rapporteringsperingsperioder`() {
        val deltakelsePeriodInfos = listOf(
            UngdomsprogramDeltakelseDAO(
                id = UUID.randomUUID(),
                deltakerIdent = "123",
                periode = Range.closed(LocalDate.parse("2024-01-15"), LocalDate.parse("2024-06-15")),
                harSøkt = false,
                opprettetTidspunkt = ZonedDateTime.now(),
                endretTidspunkt = null
            )
        ).somDeltakelsePeriodInfo()

        assertThat(deltakelsePeriodInfos).hasSize(1)
        val rapporteringsPerioder = deltakelsePeriodInfos[0].rapporteringsPerioder
        assertThat(rapporteringsPerioder).hasSize(6)

        assertEquals(rapporteringsPerioder.first().fraOgMed, LocalDate.parse("2024-01-15"))
        assertEquals(rapporteringsPerioder.last().tilOgMed, LocalDate.parse("2024-06-15"))
    }
}
