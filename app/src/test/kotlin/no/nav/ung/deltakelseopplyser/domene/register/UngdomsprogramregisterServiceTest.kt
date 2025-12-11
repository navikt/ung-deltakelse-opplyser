package no.nav.ung.deltakelseopplyser.domene.register

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import no.nav.pdl.generated.enums.IdentGruppe
import no.nav.pdl.generated.hentident.IdentInformasjon
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerRepository
import no.nav.ung.deltakelseopplyser.domene.deltaker.Scenarioer
import no.nav.ung.deltakelseopplyser.domene.inntekt.RapportertInntektService
import no.nav.ung.deltakelseopplyser.domene.minside.MineSiderService
import no.nav.ung.deltakelseopplyser.integration.abac.SifAbacPdpService
import no.nav.ung.deltakelseopplyser.integration.kontoregister.KontoregisterService
import no.nav.ung.deltakelseopplyser.integration.pdl.api.PdlService
import no.nav.ung.deltakelseopplyser.integration.ungsak.UngSakService
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseDTO
import no.nav.ung.deltakelseopplyser.kontrakt.veileder.EndrePeriodeDatoDTO
import no.nav.ung.deltakelseopplyser.statistikk.bigquery.BigQueryTestConfiguration
import no.nav.ung.deltakelseopplyser.utils.FødselsnummerGenerator
import no.nav.ung.deltakelseopplyser.utils.TokenTestUtils.mockContext
import org.assertj.core.api.Assertions.assertThat
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
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.ErrorResponseException
import java.time.LocalDate
import java.util.*


@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableMockOAuth2Server
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension::class)
@Import(BigQueryTestConfiguration::class)
class UngdomsprogramregisterServiceTest {

    @Autowired
    private lateinit var deltakerRepository: DeltakerRepository

    @Autowired
    lateinit var ungdomsprogramregisterService: UngdomsprogramregisterService

    @Autowired
    lateinit var deltakelseRepository: DeltakelseRepository

    @MockkBean
    lateinit var mineSiderService: MineSiderService

    @MockkBean(relaxed = true)
    lateinit var ungSakService: UngSakService

    @MockkBean(relaxed = true)
    lateinit var kontoregisterService: KontoregisterService

    @MockkBean(relaxed = true)
    lateinit var pdlService: PdlService


    @MockkBean(relaxed = true)
    lateinit var sifAbacPdpService: SifAbacPdpService

    @MockkBean
    lateinit var rapportertInntektService: RapportertInntektService

    @MockkBean
    lateinit var springTokenValidationContextHolder: SpringTokenValidationContextHolder

    val defaultFødselsdato =  LocalDate.of(2000, 1, 1)

    @BeforeEach
    fun setUp() {
        justRun { mineSiderService.opprettVarsel(any(), any(), any(), any(), any(), any()) }
        springTokenValidationContextHolder.mockContext()
        every { pdlService.hentPerson(any()) } returns Scenarioer.lagPerson(defaultFødselsdato)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(UngdomsprogramregisterServiceTest::class.java)
    }

    @Test
    fun `Deltaker blir meldt inn i programmet uten en sluttdato`() {
        val deltakerDTO = DeltakerDTO(deltakerIdent = FødselsnummerGenerator.neste())
        val dto = DeltakelseDTO(
            deltaker = deltakerDTO,
            fraOgMed = LocalDate.now(),
            tilOgMed = null
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
        val dto = DeltakelseDTO(
            deltaker = deltakerDTO,
            fraOgMed = mandag,
            tilOgMed = null
        )

        every { pdlService.hentFolkeregisteridenter(any()) } returns listOf(
            IdentInformasjon("02499435811", false, IdentGruppe.FOLKEREGISTERIDENT),
            IdentInformasjon("451", true, IdentGruppe.FOLKEREGISTERIDENT)
        )

        ungdomsprogramregisterService.leggTilIProgram(dto)

        // Skal feile fordi deltaker allerede er meldt inn i programmet uten t.o.m dato.
        assertThrows<DataIntegrityViolationException> {
            ungdomsprogramregisterService.leggTilIProgram(dto.copy(fraOgMed = onsdag))
        }
    }

    @Test
    fun `Innmelding av deltakelse med fraOgMed dato før programdato skal feile`() {
        val programDato = LocalDate.parse("2024-01-01")
        val dto = DeltakelseDTO(
            deltaker = DeltakerDTO(UUID.randomUUID(), "02499435811"),
            fraOgMed = programDato.minusDays(2),
            tilOgMed = null
        )

        every { pdlService.hentFolkeregisteridenter(any()) } returns listOf(
            IdentInformasjon("02499435811", false, IdentGruppe.FOLKEREGISTERIDENT),
            IdentInformasjon("451", true, IdentGruppe.FOLKEREGISTERIDENT)
        )

        assertThrows<ErrorResponseException> {
            ungdomsprogramregisterService.leggTilIProgram(dto)
        }.also {
            assertThat(it.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(it.body.detail).isEqualTo("Oppgitt dato=2023-12-30 er utenfor tillatt periode 2024-01-01..2028-12-31")
        }
    }


    @Test
    fun `Innmelding av deltakelse med fraOgMed dato på eller etter 29 årsdag skal feile`() {
        val tjuveniårsdag = defaultFødselsdato.plusYears(29)
        val dto = DeltakelseDTO(
            deltaker = DeltakerDTO(UUID.randomUUID(), "02499435811"),
            fraOgMed = tjuveniårsdag,
            tilOgMed = null
        )

        every { pdlService.hentFolkeregisteridenter(any()) } returns listOf(
            IdentInformasjon("02499435811", false, IdentGruppe.FOLKEREGISTERIDENT),
            IdentInformasjon("451", true, IdentGruppe.FOLKEREGISTERIDENT)
        )

        assertThrows<ErrorResponseException> {
            ungdomsprogramregisterService.leggTilIProgram(dto)
        }.also {
            assertThat(it.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(it.body.detail).isEqualTo("Oppgitt dato=2029-01-01 er utenfor tillatt periode 2024-01-01..2028-12-31")
        }
    }

    @Test
    fun `Deltaker blir meldt inn i programmet med en sluttdato`() {
        val deltakerDTO = DeltakerDTO(deltakerIdent = FødselsnummerGenerator.neste())
        val dto = DeltakelseDTO(
            deltaker = deltakerDTO,
            fraOgMed = LocalDate.now(),
            tilOgMed = LocalDate.now().plusDays(10)
        )
        val innmelding = ungdomsprogramregisterService.leggTilIProgram(dto)

        assertNotNull(innmelding)
        assertNotNull(innmelding.id)
        assertNotNull(innmelding.deltaker.id)
    }

    @Test
    fun `Deltaker blir fjernet fra programmet`() {
        val deltakerDTO = DeltakerDTO(deltakerIdent = FødselsnummerGenerator.neste())
        val dto = DeltakelseDTO(
            deltaker = deltakerDTO,
            fraOgMed = LocalDate.now(),
            tilOgMed = null
        )
        val innmelding = ungdomsprogramregisterService.leggTilIProgram(dto)

        val deltakerDAO = deltakerRepository.finnDeltakerGittIdenter(listOf(innmelding.deltaker.deltakerIdent)).firstOrNull()
        assertThat(deltakerDAO).isNotNull
        assertThat(deltakelseRepository.findByDeltaker_IdIn(listOf(innmelding.deltaker.id!!))).isNotEmpty

        val utmelding = ungdomsprogramregisterService.fjernFraProgram(deltakerDAO!!)

        assertTrue(utmelding)
    }

    @Test
    fun `Henter deltaker fra programmet`() {
        val deltakerDTO = DeltakerDTO(deltakerIdent = FødselsnummerGenerator.neste())
        val dto = DeltakelseDTO(
            deltaker = deltakerDTO,
            fraOgMed = LocalDate.now(),
            tilOgMed = null
        )
        val innmelding = ungdomsprogramregisterService.leggTilIProgram(dto)

        val hentetDto = ungdomsprogramregisterService.hentFraProgram(innmelding.id!!)

        assertNotNull(hentetDto)
        assertNotNull(hentetDto.deltaker.id)
    }

    @Test
    fun `Endring av startdato på deltakelse oppretter BEKREFT_ENDRET_STARTDATO oppgave`() {
        every { pdlService.hentAktørIder(any()) } returns listOf(
            IdentInformasjon("321", false, IdentGruppe.AKTORID),
            IdentInformasjon("451", true, IdentGruppe.AKTORID)
        )

        val mandag = LocalDate.parse("2024-10-07")
        val onsdag = LocalDate.parse("2024-10-09")

        val deltakerDTO = DeltakerDTO(deltakerIdent = FødselsnummerGenerator.neste())
        val dto = DeltakelseDTO(
            deltaker = deltakerDTO,
            fraOgMed = mandag,
            tilOgMed = null
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
    fun `Endring av startdato til før første mulige innmeldingsdato gir valideringsfeil`() {
        every { pdlService.hentAktørIder(any()) } returns listOf(
            IdentInformasjon("321", false, IdentGruppe.AKTORID),
            IdentInformasjon("451", true, IdentGruppe.AKTORID)
        )

        val mandag = LocalDate.parse("2024-10-07")

        val deltakerDTO = DeltakerDTO(deltakerIdent = FødselsnummerGenerator.neste())
        val dto = DeltakelseDTO(
            deltaker = deltakerDTO,
            fraOgMed = mandag,
            tilOgMed = null
        )
        val innmelding = ungdomsprogramregisterService.leggTilIProgram(dto)

        assertThrows<ErrorResponseException> {
            ungdomsprogramregisterService.endreStartdato(innmelding.id!!, mockEndrePeriodeDTO(LocalDate.parse("2023-12-31")))
        }.also {
            assertThat(it.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(it.body.detail).isEqualTo("Oppgitt dato=2023-12-31 er utenfor tillatt periode 2024-01-01..2028-12-31")
        }
    }

    @Test
    fun `Endring av sluttdato på deltakelse oppretter BEKREFT_ENDRET_SLUTTDATO oppgave`() {
        val mandag = LocalDate.parse("2024-10-07")
        val onsdag = LocalDate.parse("2024-10-09")

        val deltakerDTO = DeltakerDTO(deltakerIdent = FødselsnummerGenerator.neste())
        val dto = DeltakelseDTO(
            deltaker = deltakerDTO,
            fraOgMed = mandag,
            tilOgMed = null
        )
        val innmelding = ungdomsprogramregisterService.leggTilIProgram(dto)

        every { pdlService.hentAktørIder(any()) } returns listOf(
            IdentInformasjon("321", false, IdentGruppe.AKTORID),
            IdentInformasjon("451", true, IdentGruppe.AKTORID)
        )

        val oppdatertDto = DeltakelseDTO(
            deltaker = innmelding.deltaker,
            fraOgMed = mandag,
            tilOgMed = onsdag
        )
        ungdomsprogramregisterService.avsluttDeltakelse(innmelding.id!!, oppdatertDto)

        val endretSluttdatoDeltakelse =
            ungdomsprogramregisterService.endreSluttdato(innmelding.id!!, mockEndrePeriodeDTO(onsdag.plusWeeks(1)))

        assertNotNull(endretSluttdatoDeltakelse)
        assertEquals(innmelding.deltaker, endretSluttdatoDeltakelse.deltaker)
        assertThat(endretSluttdatoDeltakelse.fraOgMed).isEqualTo(mandag)
        assertThat(endretSluttdatoDeltakelse.tilOgMed).isEqualTo(onsdag.plusWeeks(1))
    }

    @Test
    fun `Endring av sluttdato til etter siste mulige innmeldingsdato gir valideringsfeil`() {
        val mandag = LocalDate.parse("2024-10-07")
        val onsdag = LocalDate.parse("2024-10-09")

        val deltakerDTO = DeltakerDTO(deltakerIdent = FødselsnummerGenerator.neste())
        val dto = DeltakelseDTO(
            deltaker = deltakerDTO,
            fraOgMed = mandag,
            tilOgMed = null
        )
        val innmelding = ungdomsprogramregisterService.leggTilIProgram(dto)

        every { pdlService.hentAktørIder(any()) } returns listOf(
            IdentInformasjon("321", false, IdentGruppe.AKTORID),
            IdentInformasjon("451", true, IdentGruppe.AKTORID)
        )

        val oppdatertDto = DeltakelseDTO(
            deltaker = innmelding.deltaker,
            fraOgMed = mandag,
            tilOgMed = onsdag
        )
        ungdomsprogramregisterService.avsluttDeltakelse(innmelding.id!!, oppdatertDto)

        assertThrows<ErrorResponseException> {
            ungdomsprogramregisterService.endreSluttdato(innmelding.id!!, mockEndrePeriodeDTO(LocalDate.parse("2029-01-01")))
        }
    }

    @Test
    fun `Deltaker blir meldt inn to ganger ved feil skal ikke produsere to oppgaver`() {
        val deltakerDTO = DeltakerDTO(deltakerIdent = FødselsnummerGenerator.neste())
        val dto = DeltakelseDTO(
            deltaker = deltakerDTO,
            fraOgMed = LocalDate.now(),
            tilOgMed = null
        )
        ungdomsprogramregisterService.leggTilIProgram(dto)
        assertThrows<DataIntegrityViolationException> { ungdomsprogramregisterService.leggTilIProgram(dto) }
        verify(exactly = 1) { mineSiderService.opprettVarsel(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `Deltaker blir fjernet fra programmet_etter_søkt_ytelse`() {
        val deltakerIdent = FødselsnummerGenerator.neste()
        val deltakerDTO = DeltakerDTO(deltakerIdent = deltakerIdent)
        val dto = DeltakelseDTO(
            deltaker = deltakerDTO,
            fraOgMed = LocalDate.now(),
            tilOgMed = null
        )
        every { pdlService.hentAktørIder(any()) } returns listOf(
            IdentInformasjon("321", false, IdentGruppe.AKTORID),
            IdentInformasjon("451", true, IdentGruppe.AKTORID)
        )
        every { pdlService.hentFolkeregisteridenter(any()) } returns listOf(
            IdentInformasjon(deltakerIdent, false, IdentGruppe.FOLKEREGISTERIDENT),
        )

        val innmelding = ungdomsprogramregisterService.leggTilIProgram(dto)
        ungdomsprogramregisterService.markerSomHarSøkt(innmelding.id!!)

        val deltakerDAO = deltakerRepository.finnDeltakerGittIdenter(listOf(innmelding.deltaker.deltakerIdent)).firstOrNull()
        assertThat(deltakerDAO).isNotNull
        assertThat(deltakelseRepository.findByDeltaker_IdIn(listOf(innmelding.deltaker.id!!))).isNotEmpty

        assertThat(ungdomsprogramregisterService.hentIkkeSlettetForDeltakerId(deltakerDAO!!.id).isNotEmpty())

        val utmelding = ungdomsprogramregisterService.fjernFraProgram(deltakerDAO)
        assertTrue(utmelding)

        assertThat(ungdomsprogramregisterService.hentIkkeSlettetForDeltakerId(deltakerDAO.id).isEmpty())
    }

    private fun mockEndrePeriodeDTO(dato: LocalDate) = EndrePeriodeDatoDTO(dato = dato)
}
