package no.nav.ung.deltakelseopplyser.integration.pdl.api

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.ninjasquad.springmockk.MockkBean
import no.nav.pdl.generated.enums.IdentGruppe
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.ung.deltakelseopplyser.statistikk.bigquery.BigQueryTestConfiguration
import no.nav.ung.deltakelseopplyser.utils.FødselsnummerGenerator
import no.nav.ung.deltakelseopplyser.utils.TokenTestUtils.mockContext
import no.nav.ung.deltakelseopplyser.wiremock.AutoConfigureWireMock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
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
class PdlServiceTest {

    @Autowired
    private lateinit var pdlService: PdlService

    @Autowired
    private lateinit var wireMockServer: WireMockServer

    @MockkBean
    private lateinit var springTokenValidationContextHolder: SpringTokenValidationContextHolder

    private companion object {
        const val PDL_GRAPHQL_PATH = "/pdl-api-mock/graphql"
    }

    @BeforeEach
    fun setUp() {
        springTokenValidationContextHolder.mockContext()
    }

    @Test
    fun `hentPerson returnerer person ved OK-respons`() {
        val fnr = FødselsnummerGenerator.neste()
        stubPdlPost(
            // language=json
            """
            {
              "data": {
                "hentPerson": {
                  "folkeregisteridentifikator": [{"identifikasjonsnummer": "$fnr"}],
                  "navn": [{"fornavn": "Ola", "mellomnavn": null, "etternavn": "Nordmann"}],
                  "foedselsdato": [{"foedselsdato": "2000-01-01", "foedselsaar": 2000}]
                }
              }
            }
            """.trimIndent()
        )

        val person = pdlService.hentPerson(fnr)

        assertThat(person.folkeregisteridentifikator).hasSize(1)
        assertThat(person.folkeregisteridentifikator.first().identifikasjonsnummer).isEqualTo(fnr)
        assertThat(person.navn.first().fornavn).isEqualTo("Ola")
        assertThat(person.navn.first().etternavn).isEqualTo("Nordmann")
        assertThat(person.foedselsdato.first().foedselsdato).isEqualTo("2000-01-01")
    }

    @Test
    fun `hentPerson kaster IllegalStateException ved feilrespons fra PDL`() {
        stubPdlPost(
            // language=json
            """
            {
              "errors": [{"message": "Fant ikke person", "locations": [], "path": ["hentPerson"], "extensions": {"code": "not_found"}}],
              "data": null
            }
            """.trimIndent()
        )

        assertThrows<IllegalStateException> { pdlService.hentPerson("12345678901") }
    }

    @Test
    fun `hentIdenter returnerer identliste med alle identer ved OK-respons`() {
        val fnr = FødselsnummerGenerator.neste()
        val aktørId = "9876543210123"
        stubPdlPost(
            // language=json
            """
            {
              "data": {
                "hentIdenter": {
                  "identer": [
                    {"ident": "$fnr", "historisk": false, "gruppe": "FOLKEREGISTERIDENT"},
                    {"ident": "$aktørId", "historisk": false, "gruppe": "AKTORID"}
                  ]
                }
              }
            }
            """.trimIndent()
        )

        val identliste = pdlService.hentIdenter(fnr)

        assertThat(identliste.identer).hasSize(2)
        assertThat(identliste.identer.map { it.ident }).containsExactlyInAnyOrder(fnr, aktørId)
    }

    @Test
    fun `hentFolkeregisteridenter returnerer kun FOLKEREGISTERIDENT-innslag`() {
        val fnr = FødselsnummerGenerator.neste()
        stubPdlPost(
            // language=json
            """
            {
              "data": {
                "hentIdenter": {
                  "identer": [
                    {"ident": "$fnr", "historisk": false, "gruppe": "FOLKEREGISTERIDENT"},
                    {"ident": "9876543210123", "historisk": false, "gruppe": "AKTORID"}
                  ]
                }
              }
            }
            """.trimIndent()
        )

        val folkeregisteridenter = pdlService.hentFolkeregisteridenter(fnr)

        assertThat(folkeregisteridenter).hasSize(1)
        assertThat(folkeregisteridenter.first().gruppe).isEqualTo(IdentGruppe.FOLKEREGISTERIDENT)
        assertThat(folkeregisteridenter.first().ident).isEqualTo(fnr)
    }

    @Test
    fun `hentAktørIder returnerer kun AKTORID-innslag`() {
        val fnr = FødselsnummerGenerator.neste()
        val aktørId = "9876543210123"
        stubPdlPost(
            // language=json
            """
            {
              "data": {
                "hentIdenter": {
                  "identer": [
                    {"ident": "$fnr", "historisk": false, "gruppe": "FOLKEREGISTERIDENT"},
                    {"ident": "$aktørId", "historisk": false, "gruppe": "AKTORID"}
                  ]
                }
              }
            }
            """.trimIndent()
        )

        val aktørIder = pdlService.hentAktørIder(fnr)

        assertThat(aktørIder).hasSize(1)
        assertThat(aktørIder.first().gruppe).isEqualTo(IdentGruppe.AKTORID)
        assertThat(aktørIder.first().ident).isEqualTo(aktørId)
    }

    private fun stubPdlPost(responseBody: String) {
        wireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(PDL_GRAPHQL_PATH))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)
                )
        )
    }
}
