package no.nav.ung.deltakelseopplyser.integration.ungsak

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.ninjasquad.springmockk.MockkBean
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OppgaveYtelsetype
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OpprettOppgaveDto
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.søkytelse.SøkYtelseOppgavetypeDataDto
import no.nav.ung.brukerdialog.typer.AktørId
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
        const val OPPRETT_SØK_YTELSE_PATH = "/ung-brukerdialog-api-mock/ung/brukerdialog/intern/api/oppgavebehandling/opprett"
    }

    @Test
    fun `opprettSøkYtelseOppgave returnerer true ved vellykket opprettelse`() {
        wireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(OPPRETT_SØK_YTELSE_PATH))
                .willReturn(WireMock.aResponse().withStatus(HttpStatus.OK.value()))
        )

        val resultat = ungBrukerdialogService.opprettSøkYtelseOppgave(
            OpprettOppgaveDto(
                AktørId("1234567890123"),
                OppgaveYtelsetype.UNGDOMSYTELSE,
                UUID.randomUUID(),
                SøkYtelseOppgavetypeDataDto(LocalDate.now()),
                null
            )
        )

        assertThat(resultat).isTrue()
    }

    @Test
    fun `opprettSøkYtelseOppgave returnerer false ved klientfeil`() {
        wireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(OPPRETT_SØK_YTELSE_PATH))
                .willReturn(WireMock.aResponse().withStatus(HttpStatus.BAD_REQUEST.value()))
        )

        val resultat = ungBrukerdialogService.opprettSøkYtelseOppgave(
            OpprettOppgaveDto(
                AktørId("1234567890123"),
                OppgaveYtelsetype.UNGDOMSYTELSE,
                UUID.randomUUID(),
                SøkYtelseOppgavetypeDataDto(LocalDate.now()),
                null
            )
        )

        assertThat(resultat).isFalse()
    }
}
