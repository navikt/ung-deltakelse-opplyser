package no.nav.ung.deltakelseopplyser.register

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
import org.springframework.web.ErrorResponseException
import java.time.LocalDate


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
    lateinit var repository: UngdomsprogramRepository

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
        val dto = DeltakerProgramOpplysningDTO(
            deltakerIdent = "123",
            fraOgMed = LocalDate.now(),
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

        val dto = DeltakerProgramOpplysningDTO(
            deltakerIdent = "123",
            fraOgMed = mandag,
            tilOgMed = null
        )
        val deltakelse = ungdomsprogramregisterService.leggTilIProgram(dto)

        // Skal feile fordi deltaker allerede er meldt inn i programmet uten t.o.m dato.
        assertThrows<ErrorResponseException> { ungdomsprogramregisterService.leggTilIProgram(dto) }

        // Skal feile fordi deltaker allerede er meldt inn i programmet med t.o.m dato.
        ungdomsprogramregisterService.oppdaterProgram(deltakelse.id!!, dto.copy(tilOgMed = onsdag))
        assertThrows<ErrorResponseException> { ungdomsprogramregisterService.leggTilIProgram(dto) }
    }

    @Test
    fun `Deltaker blir meldt inn i programmet med en sluttdato`() {
        val dto = DeltakerProgramOpplysningDTO(
            deltakerIdent = "123",
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
        val dto = DeltakerProgramOpplysningDTO(
            deltakerIdent = "123",
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

        val dto = DeltakerProgramOpplysningDTO(
            deltakerIdent = "123",
            fraOgMed = mandag,
            tilOgMed = null
        )
        val innmelding = ungdomsprogramregisterService.leggTilIProgram(dto)

        val oppdatertDto = DeltakerProgramOpplysningDTO(
            deltakerIdent = "123",
            fraOgMed = mandag,
            tilOgMed = onsdag
        )
        val oppdatertInnmelding = ungdomsprogramregisterService.oppdaterProgram(innmelding.id!!, oppdatertDto)

        assertNotNull(oppdatertInnmelding)
        assertEquals(oppdatertDto.deltakerIdent, oppdatertInnmelding.deltakerIdent)
        assertEquals(oppdatertDto.tilOgMed, oppdatertInnmelding.tilOgMed)
    }

    @Test
    fun `Henter deltaker fra programmet`() {
        val dto = DeltakerProgramOpplysningDTO(
            deltakerIdent = "123",
            fraOgMed = LocalDate.now(),
            tilOgMed = null
        )
        val innmelding = ungdomsprogramregisterService.leggTilIProgram(dto)

        val hentetDto = ungdomsprogramregisterService.hentFraProgram(innmelding.id!!)

        assertNotNull(hentetDto)
        assertEquals(dto.deltakerIdent, hentetDto.deltakerIdent)
    }
}
