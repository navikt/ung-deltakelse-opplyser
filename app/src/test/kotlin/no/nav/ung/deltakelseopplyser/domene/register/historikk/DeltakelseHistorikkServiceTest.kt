package no.nav.ung.deltakelseopplyser.domene.register.historikk

import com.ninjasquad.springmockk.MockkBean
import io.hypersistence.utils.hibernate.type.range.Range
import io.mockk.every
import io.mockk.justRun
import no.nav.pdl.generated.enums.IdentGruppe
import no.nav.pdl.generated.hentident.IdentInformasjon
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerRepository
import no.nav.ung.deltakelseopplyser.domene.minside.MineSiderService
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramDeltakelseRepository
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService
import no.nav.ung.deltakelseopplyser.domene.register.historikk.DeltakelseHistorikk.Companion.DATE_TIME_FORMATTER
import no.nav.ung.deltakelseopplyser.integration.pdl.api.PdlService
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseOpplysningDTO
import no.nav.ung.deltakelseopplyser.kontrakt.register.historikk.Endringstype
import no.nav.ung.deltakelseopplyser.kontrakt.register.historikk.Revisjonstype
import no.nav.ung.deltakelseopplyser.kontrakt.veileder.EndrePeriodeDatoDTO
import no.nav.ung.deltakelseopplyser.utils.TokenTestUtils.mockContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.listener.KafkaExceptionLogLevelAware
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableMockOAuth2Server
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension::class)
class DeltakelseHistorikkServiceTest {

    @Autowired
    private lateinit var kafkaExceptionLogLevelAware: KafkaExceptionLogLevelAware

    @Autowired
    private lateinit var deltakerRepository: DeltakerRepository

    @Autowired
    private lateinit var deltakelseRepository: UngdomsprogramDeltakelseRepository

    @Autowired
    private lateinit var deltakelseHistorikkService: DeltakelseHistorikkService

    @Autowired
    lateinit var ungdomsprogramregisterService: UngdomsprogramregisterService

    @MockkBean
    lateinit var springTokenValidationContextHolder: SpringTokenValidationContextHolder

    @MockkBean(relaxed = true)
    private lateinit var pdlService: PdlService

    @MockkBean
    private lateinit var mineSiderService: MineSiderService

    @BeforeEach
    fun setUp() {
        deltakelseRepository.deleteAll()
        deltakerRepository.deleteAll()

        justRun { mineSiderService.opprettVarsel(any(), any(), any(), any(), any(), any()) }
        springTokenValidationContextHolder.mockContext()
    }

    @AfterAll
    fun tearDown() {
        deltakelseRepository.deleteAll()
        deltakerRepository.deleteAll()
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(DeltakelseHistorikkServiceTest::class.java)
    }

    @Test
    fun `Deltaker blir meldt inn i programmet, startdato endres, deltaker søker ytelse, og deretter meldes deltaker ut av programmet`() {
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
        val innmelding = ungdomsprogramregisterService.leggTilIProgram(dto) // Fører til første historikkinnslag
        assertThat(innmelding.id).isNotNull
        val deltakelseId = innmelding.id!!

        ungdomsprogramregisterService.endreStartdato(
            deltakelseId,
            EndrePeriodeDatoDTO(onsdag)
        ) // Fører til andre historikkinnslag

        val søktTidspunkt =
            ungdomsprogramregisterService.markerSomHarSøkt(deltakelseId).søktTidspunkt // Fører til tredje historikkinnslag

        ungdomsprogramregisterService.avsluttDeltakelse(
            deltakelseId, DeltakelseOpplysningDTO(
                deltaker = innmelding.deltaker,
                fraOgMed = onsdag,
                tilOgMed = onsdag,
                oppgaver = listOf()
            )
        ) // Fører til fjerde historikkinnslag

        ungdomsprogramregisterService.endreSluttdato(
            deltakelseId,
            EndrePeriodeDatoDTO(onsdag.plusWeeks(1))
        ) // Fører til femte historikkinnslag

        val historikk = deltakelseHistorikkService.deltakelseHistorikk(deltakelseId)
        assertThat(historikk).hasSize(5).also {
            historikk.forEach { logger.info("Innslag: {}", it.utledEndringsTekst()) }
        }

        val innslag = historikk.iterator()

        val førsteInnslag = innslag.next() // Første innslag er opprettelse av deltakelse
        assertThat(førsteInnslag.revisjonsnummer).isNotNull()
        assertThat(førsteInnslag.revisjonstype).isEqualTo(Revisjonstype.OPPRETTET)
        assertThat(førsteInnslag.endringstype).isEqualTo(Endringstype.DELTAKER_MELDT_INN)
        assertThat(førsteInnslag.deltakelse.getFom()).isEqualTo(mandag)
        assertThat(førsteInnslag.deltakelse.getTom()).isNull()
        assertThat(førsteInnslag.opprettetAv).isNotNull()
        assertThat(førsteInnslag.opprettetTidspunkt).isNotNull()
        assertThat(førsteInnslag.endretAv).isNotNull()
        assertThat(førsteInnslag.endretTidspunkt).isNotNull()
        assertThat(førsteInnslag.utledEndringsTekst()).isEqualTo("Deltaker er meldt inn i programmet.")

        val andreInnslag = innslag.next() // Andre innslag er endring av startdato
        assertThat(andreInnslag.revisjonsnummer).isGreaterThan(førsteInnslag.revisjonsnummer)
        assertThat(andreInnslag.revisjonstype).isEqualTo(Revisjonstype.ENDRET)
        assertThat(andreInnslag.endringstype).isEqualTo(Endringstype.ENDRET_STARTDATO)
        assertThat(andreInnslag.endretStartdato).isNotNull
        assertThat(andreInnslag.endretStartdato!!.nyStartdato).isEqualTo(onsdag)
        assertThat(andreInnslag.endretStartdato!!.gammelStartdato).isEqualTo(mandag)
        assertThat(andreInnslag.deltakelse.getTom()).isNull()
        assertThat(andreInnslag.opprettetAv).isEqualTo(førsteInnslag.opprettetAv)
        assertThat(andreInnslag.opprettetTidspunkt).isEqualTo(førsteInnslag.opprettetTidspunkt)
        assertThat(andreInnslag.endretAv).isNotNull()
        assertThat(andreInnslag.endretTidspunkt).isNotNull()
        assertThat(andreInnslag.utledEndringsTekst()).isEqualTo("Startdato for deltakelse er endret fra $mandag til $onsdag.")

        val tredjeInnslag = innslag.next() // Tredje innslag er deltaker som har søkt ytelse
        assertThat(tredjeInnslag.revisjonsnummer).isGreaterThan(andreInnslag.revisjonsnummer)
        assertThat(tredjeInnslag.revisjonstype).isEqualTo(Revisjonstype.ENDRET)
        assertThat(tredjeInnslag.endringstype).isEqualTo(Endringstype.DELTAKER_HAR_SØKT_YTELSE)
        assertThat(tredjeInnslag.søktTidspunktSatt).isNotNull
        assertThat(tredjeInnslag.søktTidspunktSatt!!.søktTidspunkt).isNotNull()
        assertThat(tredjeInnslag.søktTidspunktSatt!!.søktTidspunktSatt).isTrue()
        assertThat(tredjeInnslag.deltakelse.getFom()).isEqualTo(onsdag)
        assertThat(tredjeInnslag.deltakelse.getTom()).isNull()
        assertThat(tredjeInnslag.opprettetAv).isEqualTo(førsteInnslag.opprettetAv)
        assertThat(tredjeInnslag.opprettetTidspunkt).isEqualTo(førsteInnslag.opprettetTidspunkt)
        assertThat(tredjeInnslag.endretAv).isNotNull()
        assertThat(tredjeInnslag.endretTidspunkt).isNotNull()
        assertThat(tredjeInnslag.utledEndringsTekst()).isEqualTo("Deltaker har søkt om ytelse den ${DATE_TIME_FORMATTER.format(søktTidspunkt)}.")

        val fjerdeInnslag = innslag.next() // Fjerde innslag er avslutning av deltakelse
        assertThat(fjerdeInnslag.revisjonsnummer).isGreaterThan(tredjeInnslag.revisjonsnummer)
        assertThat(fjerdeInnslag.revisjonstype).isEqualTo(Revisjonstype.ENDRET)
        assertThat(fjerdeInnslag.endringstype).isEqualTo(Endringstype.ENDRET_SLUTTDATO)
        assertThat(fjerdeInnslag.endretSluttdato).isNotNull
        assertThat(fjerdeInnslag.endretSluttdato!!.nySluttdato).isEqualTo(onsdag)
        assertThat(fjerdeInnslag.endretSluttdato!!.gammelSluttdato).isNull() // Sluttdato var ikke satt før
        assertThat(fjerdeInnslag.deltakelse.getFom()).isEqualTo(onsdag)
        assertThat(fjerdeInnslag.deltakelse.getTom()).isEqualTo(onsdag)
        assertThat(fjerdeInnslag.opprettetAv).isEqualTo(førsteInnslag.opprettetAv)
        assertThat(fjerdeInnslag.opprettetTidspunkt).isEqualTo(førsteInnslag.opprettetTidspunkt)
        assertThat(fjerdeInnslag.endretAv).isNotNull()
        assertThat(fjerdeInnslag.endretTidspunkt).isNotNull()
        assertThat(fjerdeInnslag.utledEndringsTekst()).isEqualTo("Sluttdato for deltakelse er satt til $onsdag.")

        val femteInnslag = innslag.next() // Femte innslag er endring av sluttdato
        assertThat(femteInnslag.revisjonsnummer).isGreaterThan(fjerdeInnslag.revisjonsnummer)
        assertThat(femteInnslag.revisjonstype).isEqualTo(Revisjonstype.ENDRET)
        assertThat(femteInnslag.endringstype).isEqualTo(Endringstype.ENDRET_SLUTTDATO)
        assertThat(femteInnslag.endretSluttdato).isNotNull
        assertThat(femteInnslag.endretSluttdato!!.nySluttdato).isEqualTo(onsdag.plusWeeks(1))
        assertThat(femteInnslag.endretSluttdato!!.gammelSluttdato).isEqualTo(onsdag)
        assertThat(femteInnslag.deltakelse.getFom()).isEqualTo(onsdag)
        assertThat(femteInnslag.deltakelse.getTom()).isEqualTo(onsdag.plusWeeks(1))
        assertThat(femteInnslag.opprettetAv).isEqualTo(førsteInnslag.opprettetAv)
        assertThat(femteInnslag.opprettetTidspunkt).isEqualTo(førsteInnslag.opprettetTidspunkt)
        assertThat(femteInnslag.endretAv).isNotNull()
        assertThat(femteInnslag.endretTidspunkt).isNotNull()
        assertThat(femteInnslag.utledEndringsTekst()).isEqualTo("Sluttdato for deltakelse er endret fra $onsdag til ${onsdag.plusWeeks(1)}.")
    }

    @Test
    fun `Flere endringer på engang skal føre til feil ved henting av historikk`() {
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
        val innmelding = ungdomsprogramregisterService.leggTilIProgram(dto) // Fører til første historikkinnslag
        assertThat(innmelding.id).isNotNull
        val deltakelseId = innmelding.id!!

        // Gjør flere endringer på engang
        val eksisterendeDeltakelse = deltakelseRepository.findById(deltakelseId).orElseThrow()
        eksisterendeDeltakelse.markerSomHarSøkt()
        eksisterendeDeltakelse.oppdaterPeriode(Range.closed(innmelding.fraOgMed, onsdag))
        deltakelseRepository.saveAndFlush(eksisterendeDeltakelse)

        // Forventer at det kastes en feil når vi prøver å hente historikken
        val unsupportedOperationException =
            assertThrows<UnsupportedOperationException> { deltakelseHistorikkService.deltakelseHistorikk(deltakelseId) }
        assertThat(unsupportedOperationException.message)
            .isEqualTo("Deltakelse med id $deltakelseId har endret sluttdato og søktTidspunkt i samme revisjon. Dette er uvanlig.")
    }
}
