package no.nav.ung.deltakelseopplyser.register

import com.ninjasquad.springmockk.MockkBean
import io.hypersistence.utils.hibernate.type.range.Range
import io.mockk.every
import io.mockk.verify
import jakarta.persistence.EntityManager
import no.nav.pdl.generated.enums.IdentGruppe
import no.nav.pdl.generated.hentident.IdentInformasjon
import no.nav.ung.deltakelseopplyser.integration.ungsak.UngSakService
import no.nav.ung.deltakelseopplyser.integration.pdl.api.PdlService
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
    lateinit var ungSakService: UngSakService

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
        val deltakerDTO = DeltakerDTO(deltakerIdent = "123")
        val dto = DeltakelseOpplysningDTO(
            deltaker = deltakerDTO,
            harSøkt = false,
            fraOgMed = LocalDate.now(),
            tilOgMed = null,
            oppgaver = listOf()
        )
        val innmelding = ungdomsprogramregisterService.leggTilIProgram(dto)

        assertNotNull(innmelding)
        assertNotNull(innmelding.id)
        assertNotNull(innmelding.deltaker?.id)
    }

    @Test
    fun `Innmelding av deltakelse med overlappende perioder feiler`() {
        val mandag = LocalDate.parse("2024-10-07")
        val onsdag = LocalDate.parse("2024-10-09")

        val deltakerDTO = DeltakerDTO(UUID.randomUUID(), "02499435811")
        val dto = DeltakelseOpplysningDTO(
            deltaker = deltakerDTO,
            harSøkt = false,
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
        val deltakerDTO = DeltakerDTO(deltakerIdent =  "123")
        val dto = DeltakelseOpplysningDTO(
            deltaker = deltakerDTO,
            harSøkt = false,
            fraOgMed = LocalDate.now(),
            tilOgMed = LocalDate.now().plusDays(10),
            oppgaver = listOf()
        )
        val innmelding = ungdomsprogramregisterService.leggTilIProgram(dto)

        assertNotNull(innmelding)
        assertNotNull(innmelding.id)
        assertNotNull(innmelding.deltaker?.id)
    }

    @Test
    fun `Deltaker blir fjernet fra programmet`() {
        val deltakerDTO = DeltakerDTO(UUID.randomUUID(), "123")
        val dto = DeltakelseOpplysningDTO(
            deltaker = deltakerDTO,
            harSøkt = false,
            fraOgMed = LocalDate.now(),
            tilOgMed = null,
            oppgaver = listOf()
        )
        val innmelding = ungdomsprogramregisterService.leggTilIProgram(dto)

        val utmelding = ungdomsprogramregisterService.fjernFraProgram(innmelding.id!!)

        assertTrue(utmelding)
    }

    @Test
    fun `Deltaker blir oppdatert i programmet`() {
        val mandag = LocalDate.parse("2024-10-07")
        val onsdag = LocalDate.parse("2024-10-09")

        val deltakerDTO = DeltakerDTO(deltakerIdent = "123")
        val dto = DeltakelseOpplysningDTO(
            deltaker = deltakerDTO,
            harSøkt = false,
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
            harSøkt = false,
            fraOgMed = mandag,
            tilOgMed = onsdag,
            oppgaver = listOf()
        )
        val oppdatertInnmelding = ungdomsprogramregisterService.oppdaterProgram(innmelding.id!!, oppdatertDto)

        assertNotNull(oppdatertInnmelding)
        assertEquals(innmelding.deltaker, oppdatertInnmelding.deltaker)
        assertEquals(oppdatertDto.tilOgMed, oppdatertInnmelding.tilOgMed)

        verify { pdlService.hentAktørIder(any(), any()) }
        verify { ungSakService.sendInnHendelse(any()) }
    }

    @Test
    fun `Henter deltaker fra programmet`() {
        val deltakerDTO = DeltakerDTO(deltakerIdent = "123")
        val dto = DeltakelseOpplysningDTO(
            deltaker = deltakerDTO,
            harSøkt = false,
            fraOgMed = LocalDate.now(),
            tilOgMed = null,
            oppgaver = listOf()
        )
        val innmelding = ungdomsprogramregisterService.leggTilIProgram(dto)

        val hentetDto = ungdomsprogramregisterService.hentFraProgram(innmelding.id!!)

        assertNotNull(hentetDto)
        assertNotNull(hentetDto.deltaker?.id)
    }

    @Test
    fun `Forventer riktig genererte rapporteringsperioder fra deltakelsesperiode`() {
        val deltakelsePeriodInfos = listOf(
            UngdomsprogramDeltakelseDAO(
                id = UUID.randomUUID(),
                deltaker = DeltakerDAO(deltakerIdent = "123"),
                periode = Range.closed(LocalDate.parse("2024-01-15"), LocalDate.parse("2024-06-15")),
                harSøkt = false,
                opprettetTidspunkt = ZonedDateTime.now(),
                endretTidspunkt = null
            )
        ).somDeltakelsePeriodInfo()

        assertThat(deltakelsePeriodInfos).hasSize(1)
        val rapporteringsPerioder = deltakelsePeriodInfos[0].rapporteringsPerioder
        assertThat(rapporteringsPerioder).hasSize(6)

        assertThat(rapporteringsPerioder.first().fraOgMed).isEqualTo(LocalDate.parse("2024-01-15"))
        assertThat(rapporteringsPerioder.first().tilOgMed).isEqualTo(LocalDate.parse("2024-01-31"))

        assertThat(rapporteringsPerioder[1].fraOgMed).isEqualTo(LocalDate.parse("2024-02-01"))
        assertThat(rapporteringsPerioder[1].tilOgMed).isEqualTo(LocalDate.parse("2024-02-29"))

        assertThat(rapporteringsPerioder[2].fraOgMed).isEqualTo(LocalDate.parse("2024-03-01"))
        assertThat(rapporteringsPerioder[2].tilOgMed).isEqualTo(LocalDate.parse("2024-03-31"))

        assertThat(rapporteringsPerioder[3].fraOgMed).isEqualTo(LocalDate.parse("2024-04-01"))
        assertThat(rapporteringsPerioder[3].tilOgMed).isEqualTo(LocalDate.parse("2024-04-30"))

        assertThat(rapporteringsPerioder[4].fraOgMed).isEqualTo(LocalDate.parse("2024-05-01"))
        assertThat(rapporteringsPerioder[4].tilOgMed).isEqualTo(LocalDate.parse("2024-05-31"))

        assertThat(rapporteringsPerioder.last().fraOgMed).isEqualTo(LocalDate.parse("2024-06-01"))
        assertThat(rapporteringsPerioder.last().tilOgMed).isEqualTo(LocalDate.parse("2024-06-15"))
    }

    @Test
    fun `Forvent at rapporteringsperiode genereres dagens dato dersom tom dato ikke er satt`() {
        val idag = LocalDate.now()
        val toMånederSiden = idag.minusMonths(2)
        val periodeFra = toMånederSiden
        val deltakelsePeriodInfos = listOf(
            UngdomsprogramDeltakelseDAO(
                id = UUID.randomUUID(),
                deltaker = DeltakerDAO(deltakerIdent = "123"),
                periode = Range.closedInfinite(periodeFra),
                harSøkt = false,
                opprettetTidspunkt = ZonedDateTime.now(),
                endretTidspunkt = null
            )
        ).somDeltakelsePeriodInfo()

        assertThat(deltakelsePeriodInfos).hasSize(1)
        val rapporteringsPerioder = deltakelsePeriodInfos[0].rapporteringsPerioder
        assertThat(rapporteringsPerioder).hasSize(3)

        assertThat(rapporteringsPerioder.first().fraOgMed).isEqualTo(toMånederSiden)
        assertThat(rapporteringsPerioder.first().tilOgMed).isEqualTo(toMånederSiden.withDayOfMonth(toMånederSiden.lengthOfMonth()))

        assertThat(rapporteringsPerioder.last().fraOgMed).isEqualTo(idag.withDayOfMonth(1))
        assertThat(rapporteringsPerioder.last().tilOgMed).isEqualTo(idag)
    }
}
