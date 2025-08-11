package no.nav.ung.deltakelseopplyser.integration.kontoregister

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.ninjasquad.springmockk.MockkBean
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.ung.deltakelseopplyser.statistikk.bigquery.BigQueryTestConfiguration
import no.nav.ung.deltakelseopplyser.utils.TokenTestUtils.mockContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.net.URI

@AutoConfigureWireMock
@EnableMockOAuth2Server
@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(BigQueryTestConfiguration::class)
class KontoregisterServiceTest {

    @Autowired
    lateinit var kontoregisterService: KontoregisterService

    @Autowired
    lateinit var wireMockServer: WireMockServer

    @MockkBean
    private lateinit var springTokenValidationContextHolder: SpringTokenValidationContextHolder

    @BeforeEach
    fun setUp() {
        springTokenValidationContextHolder.mockContext()
    }

    private companion object {
        val KONTOREGISTER_BASE_PATH = "/sokos-kontoregister-person-mock/api/borger/v1"
    }

    @Test
    fun `Forventer kontonummer gitt OK respons`() {
        mockHentKontonummer(
            statusKode = HttpStatus.OK.value(),
            // language=json
            body = """{"kontonummer":"12345678901"}"""
        )

        val kontonummerDTO = kontoregisterService.hentAktivKonto()
        assertThat(kontonummerDTO.harKontonummer).isTrue()
        assertThat(kontonummerDTO.kontonummer).isEqualTo("12345678901")
    }

    @Test
    fun `Forventer ikke kontonummer gitt ikke-funnet respons`() {
        mockHentKontonummer(
            statusKode = HttpStatus.NOT_FOUND.value(),
            // language=json
            body = """{"feilmelding": "person ikke funnet i kontoregister"}"""
        )

        val kontonummerDTO = kontoregisterService.hentAktivKonto()
        assertThat(kontonummerDTO.harKontonummer).isFalse()
        assertThat(kontonummerDTO.kontonummer).isNull()

        verifiserAntallKall(1, "$KONTOREGISTER_BASE_PATH/hent-aktiv-konto")
    }

    @Test
    fun `Forventer at det kastes feil ved andre feil`() {
        mockHentKontonummer(
            statusKode = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            body = """{"feilmelding": "Noe har gått galt"}"""
        )

        val kontoregisterException = assertThrows<KontoregisterException> { kontoregisterService.hentAktivKonto() }
        assertThat(kontoregisterException.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(kontoregisterException.body.type).isEqualTo(URI("/problem-details/sokos-kontoregister-person"))
        assertThat(kontoregisterException.body.title).isEqualTo("Feil ved kall mot sokos-kontoregister-person")
        assertThat(kontoregisterException.body.detail).isEqualTo("Annen feil: Noe har gått galt")

        verifiserAntallKall(3, "$KONTOREGISTER_BASE_PATH/hent-aktiv-konto")
    }

    private fun verifiserAntallKall(antallKall: Int, urlPath: String) {
        wireMockServer.verify(
            antallKall,
            WireMock.getRequestedFor(WireMock.urlPathEqualTo(urlPath))
        )
    }

    private fun mockHentKontonummer(statusKode: Int, body: String?) {
        val responseDefinitionBuilder = WireMock.aResponse()
            .withStatus(statusKode)
            .withHeader("Content-Type", "application/json")

        body?.let { responseDefinitionBuilder.withBody(it) }

        wireMockServer.stubFor(
            WireMock.get(
                WireMock.urlPathEqualTo("${KONTOREGISTER_BASE_PATH}/hent-aktiv-konto")
            ).willReturn(responseDefinitionBuilder)
        )
    }
}
