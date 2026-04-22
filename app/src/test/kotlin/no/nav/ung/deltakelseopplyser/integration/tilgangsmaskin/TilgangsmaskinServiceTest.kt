package no.nav.ung.deltakelseopplyser.integration.tilgangsmaskin

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.ninjasquad.springmockk.MockkBean
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.sif.abac.kontrakt.person.PersonIdent
import no.nav.ung.deltakelseopplyser.statistikk.bigquery.BigQueryTestConfiguration
import no.nav.ung.deltakelseopplyser.utils.FødselsnummerGenerator
import no.nav.ung.deltakelseopplyser.utils.TokenTestUtils.mockContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import no.nav.ung.deltakelseopplyser.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

@AutoConfigureWireMock
@EnableMockOAuth2Server
@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = [
        "topic.listener.ung-soknad.bryter=false",
        "topic.listener.ung-oppgavebekreftelse.bryter=false",
        "topic.listener.ung-rapportert-inntekt.bryter=false",
        "topic.listener.ung-vedtakhendelse.bryter=false",
    ]
)
@Import(BigQueryTestConfiguration::class)
class TilgangsmaskinServiceTest {

    @Autowired
    private lateinit var tilgangsmaskinService: TilgangsmaskinService

    @Autowired
    private lateinit var wireMockServer: WireMockServer

    @MockkBean
    private lateinit var springTokenValidationContextHolder: SpringTokenValidationContextHolder

    private companion object {
        const val KOMPLETT_URL = "/tilgangsmaskin-mock/api/v1/komplett"
    }

    @BeforeEach
    fun setUp() {
        springTokenValidationContextHolder.mockContext()
    }

    @Test
    fun `forventer tilgang gitt 204 fra tilgangsmaskin`() {
        val personIdent = PersonIdent(FødselsnummerGenerator.neste())

        wireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(KOMPLETT_URL))
                .withRequestBody(WireMock.equalTo(personIdent.ident))
                .willReturn(WireMock.aResponse().withStatus(HttpStatus.NO_CONTENT.value()))
        )

        val beslutning = tilgangsmaskinService.evaluerKomplettRegler(personIdent)

        assertThat(beslutning.harTilgang).isTrue()
        assertThat(beslutning.avvisningsAarsak).isNull()
    }

    @Test
    fun `forventer avvist beslutning gitt 403 fra tilgangsmaskin`() {
        val personIdent = PersonIdent(FødselsnummerGenerator.neste())

        wireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(KOMPLETT_URL))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(HttpStatus.FORBIDDEN.value())
                        .withHeader("Content-Type", "application/problem+json")
                        .withBody(
                            """
                            {
                              "title": "AVVIST_STRENGT_FORTROLIG_ADRESSE",
                              "begrunnelse": "Du har ikke tilgang"
                            }
                            """.trimIndent()
                        )
                )
        )

        val beslutning = tilgangsmaskinService.evaluerKomplettRegler(personIdent)

        assertThat(beslutning.harTilgang).isFalse()
        assertThat(beslutning.avvisningsAarsak).isEqualTo("AVVIST_STRENGT_FORTROLIG_ADRESSE")
        assertThat(beslutning.begrunnelse).isEqualTo("Du har ikke tilgang")
    }
}

