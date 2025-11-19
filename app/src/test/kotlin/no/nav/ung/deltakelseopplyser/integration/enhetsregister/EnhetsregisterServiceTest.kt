package no.nav.ung.deltakelseopplyser.integration.enhetsregister

import com.github.tomakehurst.wiremock.client.WireMock
import com.ninjasquad.springmockk.MockkBean
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.ung.deltakelseopplyser.AbstractIntegrationTest
import no.nav.ung.deltakelseopplyser.integration.enhetsregisteret.EnhetsregisterException
import no.nav.ung.deltakelseopplyser.integration.enhetsregisteret.EnhetsregisterService
import no.nav.ung.deltakelseopplyser.utils.TokenTestUtils.mockContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.retry.ExhaustedRetryException
import java.net.URI

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class EnhetsregisterServiceTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var enhetsregisterService: EnhetsregisterService

    @MockkBean
    private lateinit var springTokenValidationContextHolder: SpringTokenValidationContextHolder

    override val consumerGroupPrefix: String
        get() = "EnhetsregisterServiceTest"
    override val consumerGroupTopics: List<String>
        get() = listOf()

    @BeforeEach
    fun beforeEach() {
        springTokenValidationContextHolder.mockContext()
    }

    private companion object {
        val ENHETSREGISTER_BASE_PATH = "/enhetsregister-mock/v2/organisasjon"
    }

    @Test
    fun `Forventer info gitt OK respons`() {
        mockHentOrganisasjonsinfo(
            statusKode = HttpStatus.OK.value(),
            // language=json
            body = """
                {
                    "navn": {
                        "sammensattnavn": "NAV FAMILIE- OG PENSJONSYTELSER OSLO"
                    }
                }""".trimMargin()
        )

        val organisasjonRespons = enhetsregisterService.hentOrganisasjonsinfo("123456789")
        assertThat(organisasjonRespons.navn).isNotNull
        assertThat(organisasjonRespons.navn!!.sammensattnavn).isEqualTo("NAV FAMILIE- OG PENSJONSYTELSER OSLO")
    }

    @Test
    fun `Forventer å kun forsøke 1 gang gitt ikke-funnet respons`() {
        mockHentOrganisasjonsinfo(
            statusKode = HttpStatus.NOT_FOUND.value(),
            // language=json
            body = """{"melding": "Organisasjon ikke funnet i enhetsregisteret"}"""
        )

        val exhaustedRetryException =
            assertThrows<ExhaustedRetryException> { enhetsregisterService.hentOrganisasjonsinfo("123456789") }
        assertThat(exhaustedRetryException.cause).isInstanceOf(EnhetsregisterException::class.java)

        verifiserAntallKall(1, "$ENHETSREGISTER_BASE_PATH/123456789")
    }

    @Test
    fun `Forventer at det kastes feil ved andre feil`() {
        mockHentOrganisasjonsinfo(
            statusKode = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            body = """{"melding": "Noe har gått galt"}"""
        )

        val enhetsregisterException =
            assertThrows<EnhetsregisterException> { enhetsregisterService.hentOrganisasjonsinfo("123456789") }
        assertThat(enhetsregisterException.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(enhetsregisterException.body.type).isEqualTo(URI("/problem-details/enhetsregisteret"))
        assertThat(enhetsregisterException.body.title).isEqualTo("Feil ved kall mot enhetsregisteret")
        assertThat(enhetsregisterException.body.detail).isEqualTo("Annen feil: Noe har gått galt")

        verifiserAntallKall(3, "$ENHETSREGISTER_BASE_PATH/123456789")
    }

    private fun verifiserAntallKall(antallKall: Int, urlPath: String) {
        wireMockServer.verify(
            antallKall,
            WireMock.getRequestedFor(WireMock.urlPathEqualTo(urlPath))
        )
    }

    private fun mockHentOrganisasjonsinfo(statusKode: Int, body: String?) {
        val responseDefinitionBuilder = WireMock.aResponse()
            .withStatus(statusKode)
            .withHeader("Content-Type", "application/json")

        body?.let { responseDefinitionBuilder.withBody(it) }

        wireMockServer.stubFor(
            WireMock.get(
                WireMock.urlPathMatching("${ENHETSREGISTER_BASE_PATH}/.*")
            ).willReturn(responseDefinitionBuilder)
        )
    }
}
