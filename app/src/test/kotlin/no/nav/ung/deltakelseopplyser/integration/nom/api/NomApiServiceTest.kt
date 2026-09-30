package no.nav.ung.deltakelseopplyser.integration.nom.api

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.ninjasquad.springmockk.MockkBean
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.ung.deltakelseopplyser.statistikk.bigquery.BigQueryTestConfiguration
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
import java.time.LocalDate

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
class NomApiServiceTest {

    @Autowired
    private lateinit var nomApiService: NomApiService

    @Autowired
    private lateinit var wireMockServer: WireMockServer

    @MockkBean
    private lateinit var springTokenValidationContextHolder: SpringTokenValidationContextHolder

    private companion object {
        const val NOM_GRAPHQL_PATH = "/nom-api-mock/graphql"
    }

    @BeforeEach
    fun setUp() {
        springTokenValidationContextHolder.mockContext()
    }

    @Test
    fun `hentResursserMedAlleTilknytninger returnerer ressurs med org-tilknytning ved OK-respons`() {
        val navIdent = "A123456"
        val enhetId = "enhet-uuid-1"
        stubNomPost(
            // language=json
            """
            {
              "data": {
                "ressurser": [
                  {
                    "ressurs": {
                      "navident": "$navIdent",
                      "orgTilknytning": [
                        {
                          "gyldigFom": "2020-01-01",
                          "gyldigTom": null,
                          "orgEnhet": {
                            "id": "$enhetId",
                            "remedyEnhetId": "remedy-uuid-1",
                            "navn": "NAV Oslo",
                            "gyldigFom": "2020-01-01",
                            "gyldigTom": null,
                            "orgEnhetsType": "NAV_KONTOR",
                            "agressoOrgenhetType": null
                          }
                        }
                      ]
                    }
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val resultat = nomApiService.hentResursserMedAlleTilknytninger(setOf(navIdent))

        assertThat(resultat).hasSize(1)
        val ressurs = resultat.first()
        assertThat(ressurs.navIdent).isEqualTo(navIdent)
        assertThat(ressurs.orgTilknytninger).hasSize(1)
        val tilknytning = ressurs.orgTilknytninger.first()
        assertThat(tilknytning.gyldigFom).isEqualTo(LocalDate.of(2020, 1, 1))
        assertThat(tilknytning.gyldigTom).isNull()
        assertThat(tilknytning.orgEnhet.id).isEqualTo(enhetId)
        assertThat(tilknytning.orgEnhet.navn).isEqualTo("NAV Oslo")
    }

    @Test
    fun `hentResursserMedAlleTilknytninger kaster IllegalStateException ved feilrespons fra NOM`() {
        stubNomPost(
            // language=json
            """
            {
              "errors": [{"message": "Fant ikke ressurs", "locations": [], "path": ["ressurser"], "extensions": {"code": "not_found"}}],
              "data": null
            }
            """.trimIndent()
        )

        assertThrows<IllegalStateException> {
            nomApiService.hentResursserMedAlleTilknytninger(setOf("A123456"))
        }
    }

    @Test
    fun `hentResursserMedAlleTilknytninger returnerer delmengde når færre ressurser enn forespurte navIdenter`() {
        val navIdent1 = "A111111"
        val navIdent2 = "A222222"
        stubNomPost(
            // language=json
            """
            {
              "data": {
                "ressurser": [
                  {
                    "ressurs": {
                      "navident": "$navIdent1",
                      "orgTilknytning": [
                        {
                          "gyldigFom": "2021-06-01",
                          "gyldigTom": "2023-12-31",
                          "orgEnhet": {
                            "id": "enhet-uuid-2",
                            "remedyEnhetId": null,
                            "navn": "NAV Bergen",
                            "gyldigFom": "2021-06-01",
                            "gyldigTom": "2023-12-31",
                            "orgEnhetsType": "NAV_KONTOR",
                            "agressoOrgenhetType": null
                          }
                        }
                      ]
                    }
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val resultat = nomApiService.hentResursserMedAlleTilknytninger(setOf(navIdent1, navIdent2))

        assertThat(resultat).hasSize(1)
        assertThat(resultat.first().navIdent).isEqualTo(navIdent1)
        val tilknytning = resultat.first().orgTilknytninger.first()
        assertThat(tilknytning.gyldigFom).isEqualTo(LocalDate.of(2021, 6, 1))
        assertThat(tilknytning.gyldigTom).isEqualTo(LocalDate.of(2023, 12, 31))
    }

    private fun stubNomPost(responseBody: String) {
        wireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(NOM_GRAPHQL_PATH))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)
                )
        )
    }
}
