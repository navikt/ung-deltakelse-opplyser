package no.nav.ung.deltakelseopplyser.domene.register.ungsak

import com.nimbusds.jwt.SignedJWT
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.ung.deltakelseopplyser.config.Issuers
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService
import no.nav.ung.deltakelseopplyser.integration.abac.TilgangskontrollService
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseDTO
import no.nav.ung.deltakelseopplyser.statistikk.bigquery.BigQueryTestConfiguration
import no.nav.ung.deltakelseopplyser.utils.FødselsnummerGenerator
import no.nav.ung.deltakelseopplyser.utils.TokenTestUtils.hentToken
import no.nav.ung.deltakelseopplyser.wiremock.AutoConfigureWireMock
import no.nav.ung.sak.kontrakt.person.AktørIdDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.ErrorResponseException
import java.time.LocalDate
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EnableMockOAuth2Server
@AutoConfigureWireMock
@AutoConfigureTestRestTemplate
@Import(BigQueryTestConfiguration::class)
class UngdomsprogramRegisterUngSakControllerTest {

    @Autowired
    private lateinit var testRestTemplate: TestRestTemplate

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @MockkBean
    private lateinit var tilgangskontrollService: TilgangskontrollService

    @MockkBean
    private lateinit var registerService: UngdomsprogramregisterService

    private val deltakerIdent = FødselsnummerGenerator.neste()
    private val aktørId = "1000000000001"

    // ── POST /register/{id}/marker-sokt ─────────────────────────────────────

    @Test
    fun `markerDeltakelseSomSoekt - systemtoken markerer deltakelsen som soekt`() {
        val deltakelseId = UUID.randomUUID()
        every { tilgangskontrollService.erSystemBruker() } returns true
        every { tilgangskontrollService.krevSystemtilgang() } returns Unit
        every { registerService.markerSomHarSøkt(deltakelseId) } returns DeltakelseDTO(
            id = deltakelseId,
            deltaker = DeltakerDTO(deltakerIdent = deltakerIdent),
            fraOgMed = LocalDate.of(2025, 1, 1),
            tilOgMed = null,
            harForlengetPeriode = false,
            periodeMaksDato = LocalDate.of(2026, 1, 1),
        )

        val response = testRestTemplate.exchange(
            "/register/$deltakelseId/marker-sokt",
            HttpMethod.POST,
            HttpEntity(AktørIdDto(aktørId), azureSystemToken()),
            DeltakelseDTO::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.id).isEqualTo(deltakelseId)
    }

    @Test
    fun `markerDeltakelseSomSoekt - systemtoken fra ikke-godkjent app gir 403`() {
        val deltakelseId = UUID.randomUUID()
        every { tilgangskontrollService.erSystemBruker() } returns true
        every { tilgangskontrollService.krevSystemtilgang() } throws
            ErrorResponseException(
                HttpStatus.FORBIDDEN,
                ProblemDetail.forStatusAndDetail(
                    HttpStatus.FORBIDDEN,
                    "Systemtjenesten er ikke tilgjengelig for innlogget bruker"
                ),
                null
            )

        val response = testRestTemplate.exchange(
            "/register/$deltakelseId/marker-sokt",
            HttpMethod.POST,
            HttpEntity(AktørIdDto(aktørId), azureSystemToken()),
            String::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `markerDeltakelseSomSoekt - OBO-bruker uten tilgang til aktoeren gir 403`() {
        val deltakelseId = UUID.randomUUID()
        every { tilgangskontrollService.erSystemBruker() } returns false
        every { tilgangskontrollService.krevTilgangTilPersonerForInnloggetBruker(any()) } throws
            ErrorResponseException(
                HttpStatus.FORBIDDEN,
                ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Ikke tilgang til kode6 person"),
                null
            )

        val response = testRestTemplate.exchange(
            "/register/$deltakelseId/marker-sokt",
            HttpMethod.POST,
            HttpEntity(AktørIdDto(aktørId), azureOboToken()),
            String::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `markerDeltakelseSomSoekt - OBO-bruker med tilgang markerer deltakelsen som soekt`() {
        val deltakelseId = UUID.randomUUID()
        every { tilgangskontrollService.erSystemBruker() } returns false
        every { tilgangskontrollService.krevTilgangTilPersonerForInnloggetBruker(any()) } returns Unit
        every { registerService.markerSomHarSøkt(deltakelseId) } returns DeltakelseDTO(
            id = deltakelseId,
            deltaker = DeltakerDTO(deltakerIdent = deltakerIdent),
            fraOgMed = LocalDate.of(2025, 1, 1),
            tilOgMed = null,
            harForlengetPeriode = false,
            periodeMaksDato = LocalDate.of(2026, 1, 1),
        )

        val response = testRestTemplate.exchange(
            "/register/$deltakelseId/marker-sokt",
            HttpMethod.POST,
            HttpEntity(AktørIdDto(aktørId), azureOboToken()),
            DeltakelseDTO::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.id).isEqualTo(deltakelseId)
    }

    @Test
    fun `markerDeltakelseSomSoekt - uten token gir 401`() {
        val deltakelseId = UUID.randomUUID()
        val response = testRestTemplate.exchange(
            "/register/$deltakelseId/marker-sokt",
            HttpMethod.POST,
            HttpEntity(
                AktørIdDto(aktørId),
                HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            ),
            String::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private fun azureSystemToken(): HttpHeaders =
        bearerHeaders(mockOAuth2Server.hentToken(issuerId = Issuers.AZURE, claims = mapOf("idtyp" to "app")))

    private fun azureOboToken(): HttpHeaders =
        bearerHeaders(mockOAuth2Server.hentToken(issuerId = Issuers.AZURE, claims = mapOf("NAVident" to "Z123456")))

    private fun bearerHeaders(token: SignedJWT) = HttpHeaders().apply {
        setBearerAuth(token.serialize())
        contentType = MediaType.APPLICATION_JSON
    }
}
