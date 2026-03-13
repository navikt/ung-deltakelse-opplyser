package no.nav.ung.deltakelseopplyser.integration.ungsak

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.ninjasquad.springmockk.MockkBean
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.BekreftelseDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.InntektsrapporteringOppgavetypeDataDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.KontrollerRegisterinntektOppgavetypeDataDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveStatus
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.RegisterinntektDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.YtelseRegisterInntektDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.YtelseType
import no.nav.ung.deltakelseopplyser.statistikk.bigquery.BigQueryTestConfiguration
import no.nav.ung.deltakelseopplyser.utils.TokenTestUtils.mockContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@AutoConfigureWireMock
@EnableMockOAuth2Server
@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(BigQueryTestConfiguration::class)
class UngBrukerdialogServiceTest {

    @Autowired
    lateinit var ungBrukerdialogService: UngBrukerdialogService

    @Autowired
    lateinit var wireMockServer: WireMockServer

    @MockkBean
    private lateinit var springTokenValidationContextHolder: SpringTokenValidationContextHolder

    @BeforeEach
    fun setUp() {
        springTokenValidationContextHolder.mockContext()
    }

    private companion object {
        const val MIGRER_OPPGAVER_PATH = "/ung-brukerdialog-api-mock/ung/brukerdialog/intern/api/forvaltning/oppgave/migrer"
        const val AKTØR_ID = "1234567890123"
    }

    @Test
    fun `migrerOppgaver sender alle oppgaver og returnerer vellykket resultat`() {
        stubMigrerOppgaver(HttpStatus.OK.value(), """{"antallOpprettet": 4, "antallHoppetOver": 0}""")

        val resultat = ungBrukerdialogService.migrerOppgaver(AKTØR_ID, testOppgaver())

        assertThat(resultat.antallOpprettet).isEqualTo(4)
        assertThat(resultat.antallHoppetOver).isEqualTo(0)
        verifiserAntallKall(1, MIGRER_OPPGAVER_PATH)
    }

    @Test
    fun `migrerOppgaver returnerer 0 opprettet ved klientfeil`() {
        val oppgaver = testOppgaver()
        stubMigrerOppgaver(HttpStatus.BAD_REQUEST.value(), """{"error": "bad request"}""")

        val resultat = ungBrukerdialogService.migrerOppgaver(AKTØR_ID, oppgaver)

        assertThat(resultat.antallOpprettet).isEqualTo(0)
        assertThat(resultat.antallHoppetOver).isEqualTo(oppgaver.size)
    }

    @Test
    fun `migrerOppgaver returnerer 0 opprettet ved serverfeil`() {
        val oppgaver = testOppgaver()
        stubMigrerOppgaver(HttpStatus.INTERNAL_SERVER_ERROR.value(), """{"error": "server error"}""")

        val resultat = ungBrukerdialogService.migrerOppgaver(AKTØR_ID, oppgaver)

        assertThat(resultat.antallOpprettet).isEqualTo(0)
        assertThat(resultat.antallHoppetOver).isEqualTo(oppgaver.size)
        verifiserAntallKall(3, MIGRER_OPPGAVER_PATH)
    }

    private fun testOppgaver(): List<OppgaveDTO> = listOf(
        OppgaveDTO(
            oppgaveReferanse = UUID.fromString("f8276953-6c04-4fb4-bd3c-f5dac1cbab1c"),
            oppgavetype = Oppgavetype.BEKREFT_AVVIK_REGISTERINNTEKT,
            oppgavetypeData = KontrollerRegisterinntektOppgavetypeDataDTO(
                fraOgMed = LocalDate.parse("2025-11-01"),
                tilOgMed = LocalDate.parse("2025-11-30"),
                registerinntekt = RegisterinntektDTO(
                    arbeidOgFrilansInntekter = emptyList(),
                    ytelseInntekter = listOf(YtelseRegisterInntektDTO(inntekt = 10000, ytelsetype = YtelseType.PLEIEPENGER_SYKT_BARN))
                ),
                gjelderDelerAvMåned = false
            ),
            bekreftelse = BekreftelseDTO(harUttalelse = false, uttalelseFraBruker = null),
            status = OppgaveStatus.LØST,
            opprettetDato = ZonedDateTime.parse("2026-01-07T09:20:08.354676Z"),
            løstDato = ZonedDateTime.parse("2026-01-07T09:20:24.025245Z"),
            åpnetDato = ZonedDateTime.parse("2026-01-07T09:20:17.714235Z"),
            lukketDato = null,
            frist = ZonedDateTime.parse("2026-01-08T10:20:08.074568Z")
        ),
        OppgaveDTO(
            oppgaveReferanse = UUID.fromString("2ed8d241-2c95-4b19-a789-734c809a5b54"),
            oppgavetype = Oppgavetype.RAPPORTER_INNTEKT,
            oppgavetypeData = InntektsrapporteringOppgavetypeDataDTO(
                fraOgMed = LocalDate.parse("2026-01-01"),
                tilOgMed = LocalDate.parse("2026-01-31"),
                gjelderDelerAvMåned = false
            ),
            bekreftelse = null,
            status = OppgaveStatus.UTLØPT,
            opprettetDato = ZonedDateTime.parse("2026-02-01T06:00:38.771835Z"),
            løstDato = null,
            åpnetDato = null,
            lukketDato = null,
            frist = ZonedDateTime.parse("2026-01-08T00:00:00Z")
        ),
        OppgaveDTO(
            oppgaveReferanse = UUID.fromString("1dc4a0dc-3807-4b01-b973-95dc68c2fa8a"),
            oppgavetype = Oppgavetype.BEKREFT_AVVIK_REGISTERINNTEKT,
            oppgavetypeData = KontrollerRegisterinntektOppgavetypeDataDTO(
                fraOgMed = LocalDate.parse("2025-12-01"),
                tilOgMed = LocalDate.parse("2025-12-31"),
                registerinntekt = RegisterinntektDTO(
                    arbeidOgFrilansInntekter = emptyList(),
                    ytelseInntekter = listOf(YtelseRegisterInntektDTO(inntekt = 10000, ytelsetype = YtelseType.PLEIEPENGER_SYKT_BARN))
                ),
                gjelderDelerAvMåned = false
            ),
            bekreftelse = null,
            status = OppgaveStatus.UTLØPT,
            opprettetDato = ZonedDateTime.parse("2026-01-08T06:03:05.900676Z"),
            løstDato = ZonedDateTime.parse("2026-01-09T06:03:06.262718Z"),
            åpnetDato = null,
            lukketDato = null,
            frist = ZonedDateTime.parse("2026-01-09T07:03:05.659239Z")
        ),
        OppgaveDTO(
            oppgaveReferanse = UUID.fromString("42d45b7c-6057-40f3-86c5-2bf0c7f3f94b"),
            oppgavetype = Oppgavetype.RAPPORTER_INNTEKT,
            oppgavetypeData = InntektsrapporteringOppgavetypeDataDTO(
                fraOgMed = LocalDate.parse("2026-02-01"),
                tilOgMed = LocalDate.parse("2026-02-28"),
                gjelderDelerAvMåned = false
            ),
            bekreftelse = null,
            status = OppgaveStatus.UTLØPT,
            opprettetDato = ZonedDateTime.parse("2026-03-01T06:00:46.725223Z"),
            løstDato = null,
            åpnetDato = null,
            lukketDato = null,
            frist = ZonedDateTime.parse("2026-02-08T00:00:00Z")
        )
    )

    private fun stubMigrerOppgaver(statusKode: Int, body: String?) {
        val responseDefinitionBuilder = WireMock.aResponse()
            .withStatus(statusKode)
            .withHeader("Content-Type", "application/json")
        body?.let { responseDefinitionBuilder.withBody(it) }
        wireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(MIGRER_OPPGAVER_PATH))
                .willReturn(responseDefinitionBuilder)
        )
    }

    private fun verifiserAntallKall(antallKall: Int, urlPath: String) {
        wireMockServer.verify(antallKall, WireMock.postRequestedFor(WireMock.urlPathEqualTo(urlPath)))
    }
}
