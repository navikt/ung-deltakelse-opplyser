package no.nav.ung.deltakelseopplyser.diagnostikk

import com.nimbusds.jwt.SignedJWT
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.ung.deltakelseopplyser.config.Issuers
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService
import no.nav.ung.deltakelseopplyser.drift.DiagnostikkDriftController.MarkerDeltakelseSomSøktDriftRequest
import no.nav.ung.deltakelseopplyser.integration.abac.TilgangskontrollService
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseDTO
import no.nav.ung.deltakelseopplyser.statistikk.bigquery.BigQueryTestConfiguration
import no.nav.ung.deltakelseopplyser.utils.FødselsnummerGenerator
import no.nav.ung.deltakelseopplyser.utils.TokenTestUtils.hentToken
import no.nav.ung.deltakelseopplyser.wiremock.AutoConfigureWireMock
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
class DiagnostikkDriftControllerTest {

    @Autowired
    private lateinit var testRestTemplate: TestRestTemplate

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @MockkBean
    private lateinit var tilgangskontrollService: TilgangskontrollService

    @MockkBean
    private lateinit var registerService: UngdomsprogramregisterService

    private val deltakerIdent = FødselsnummerGenerator.neste()

    // ── PATCH /diagnostikk/marker-sokt ──────────────────────────────────────

    @Test
    fun `markerDeltakelseSomSoekt - med tilgang markerer deltakelsen som soekt`() {
        val deltakelseId = UUID.randomUUID()
        every { tilgangskontrollService.krevTilgangTilPersonerForInnloggetBruker(any()) } returns Unit
        every { registerService.markerSomHarSøktForDeltaker(deltakerIdent) } returns DeltakelseDTO(
            id = deltakelseId,
            deltaker = DeltakerDTO(deltakerIdent = deltakerIdent),
            fraOgMed = LocalDate.of(2025, 1, 1),
            tilOgMed = null,
            harForlengetPeriode = false,
            periodeMaksDato = LocalDate.of(2026, 1, 1),
        )

        val response = testRestTemplate.exchange(
            "/diagnostikk/marker-sokt",
            HttpMethod.PATCH,
            HttpEntity(
                MarkerDeltakelseSomSøktDriftRequest(deltakerIdent, "manuell korrigering"),
                azureToken()
            ),
            DeltakelseDTO::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.id).isEqualTo(deltakelseId)
    }

    @Test
    fun `markerDeltakelseSomSoekt - uten tilgang gir 403`() {
        every { tilgangskontrollService.krevTilgangTilPersonerForInnloggetBruker(any()) } throws
            ErrorResponseException(
                HttpStatus.FORBIDDEN,
                ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Ikke tilgang til kode6 person"),
                null
            )

        val response = testRestTemplate.exchange(
            "/diagnostikk/marker-sokt",
            HttpMethod.PATCH,
            HttpEntity(
                MarkerDeltakelseSomSøktDriftRequest(deltakerIdent, "manuell korrigering"),
                azureToken()
            ),
            String::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `markerDeltakelseSomSoekt - ingen deltakelse funnet gir 404`() {
        every { tilgangskontrollService.krevTilgangTilPersonerForInnloggetBruker(any()) } returns Unit
        every { registerService.markerSomHarSøktForDeltaker(deltakerIdent) } throws
            ErrorResponseException(
                HttpStatus.NOT_FOUND,
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Fant ingen deltakelse for gitt deltakerIdent"),
                null
            )

        val response = testRestTemplate.exchange(
            "/diagnostikk/marker-sokt",
            HttpMethod.PATCH,
            HttpEntity(
                MarkerDeltakelseSomSøktDriftRequest(deltakerIdent, "manuell korrigering"),
                azureToken()
            ),
            String::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `markerDeltakelseSomSoekt - flere deltakelser funnet gir 400`() {
        every { tilgangskontrollService.krevTilgangTilPersonerForInnloggetBruker(any()) } returns Unit
        every { registerService.markerSomHarSøktForDeltaker(deltakerIdent) } throws
            ErrorResponseException(
                HttpStatus.BAD_REQUEST,
                ProblemDetail.forStatusAndDetail(
                    HttpStatus.BAD_REQUEST,
                    "Fant flere deltakelser for gitt deltakerIdent - kan ikke entydig avgjøre hvilken som skal markeres som søkt"
                ),
                null
            )

        val response = testRestTemplate.exchange(
            "/diagnostikk/marker-sokt",
            HttpMethod.PATCH,
            HttpEntity(
                MarkerDeltakelseSomSøktDriftRequest(deltakerIdent, "manuell korrigering"),
                azureToken()
            ),
            String::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `markerDeltakelseSomSoekt - allerede soekt gir 400`() {
        every { tilgangskontrollService.krevTilgangTilPersonerForInnloggetBruker(any()) } returns Unit
        every { registerService.markerSomHarSøktForDeltaker(deltakerIdent) } throws
            ErrorResponseException(
                HttpStatus.BAD_REQUEST,
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Deltakelsen er allerede markert som søkt"),
                null
            )

        val response = testRestTemplate.exchange(
            "/diagnostikk/marker-sokt",
            HttpMethod.PATCH,
            HttpEntity(
                MarkerDeltakelseSomSøktDriftRequest(deltakerIdent, "manuell korrigering"),
                azureToken()
            ),
            String::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `markerDeltakelseSomSoekt - uten token gir 401`() {
        val response = testRestTemplate.exchange(
            "/diagnostikk/marker-sokt",
            HttpMethod.PATCH,
            HttpEntity(
                MarkerDeltakelseSomSøktDriftRequest(deltakerIdent, "manuell korrigering"),
                HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            ),
            String::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private fun azureToken(): HttpHeaders =
        bearerHeaders(mockOAuth2Server.hentToken(issuerId = Issuers.AZURE, claims = mapOf("NAVident" to "Z123456")))

    private fun bearerHeaders(token: SignedJWT) = HttpHeaders().apply {
        setBearerAuth(token.serialize())
        contentType = MediaType.APPLICATION_JSON
    }
}
