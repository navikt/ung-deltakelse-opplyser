package no.nav.ung.deltakelseopplyser.domene.register.ekstern

import com.nimbusds.jwt.SignedJWT
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.ung.deltakelseopplyser.audit.SporingsloggService
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService
import no.nav.ung.deltakelseopplyser.integration.abac.TilgangskontrollService
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakelseSjekk
import no.nav.ung.deltakelseopplyser.statistikk.bigquery.BigQueryTestConfiguration
import no.nav.ung.deltakelseopplyser.utils.FødselsnummerGenerator
import no.nav.ung.deltakelseopplyser.utils.TokenTestUtils.hentToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.ErrorResponseException
import com.ninjasquad.springmockk.MockkBean
import no.nav.ung.deltakelseopplyser.config.Issuers
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EnableMockOAuth2Server
@AutoConfigureWireMock
@Import(BigQueryTestConfiguration::class)
class EksternDeltakelseControllerTest {

    @Autowired
    private lateinit var testRestTemplate: TestRestTemplate

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @MockkBean
    private lateinit var tilgangskontrollService: TilgangskontrollService

    @MockkBean
    private lateinit var registerService: UngdomsprogramregisterService

    @MockkBean
    private lateinit var sporingsloggService: SporingsloggService

    private val deltakerIdent = FødselsnummerGenerator.neste()

    // ── systemtoken (M2M) ────────────────────────────────────────────────────

    @Test
    fun `systemtoken - bruker er aktiv deltaker og sporingslogg hoppes over`() {
        every { tilgangskontrollService.erSystemBruker() } returns true
        every { tilgangskontrollService.krevSystemtilgang(any()) } just runs
        every { registerService.sjekkAktivDeltakelse(deltakerIdent) } returns DeltakelseSjekk(
            erDeltaker = true,
            fraOgMed = LocalDate.of(2025, 1, 1),
            tilOgMed = null
        )

        val response = testRestTemplate.exchange(
            "/ekstern/deltakelse/sjekk",
            HttpMethod.POST,
            HttpEntity(DeltakerDTO(deltakerIdent = deltakerIdent), azureSystemToken()),
            DeltakelseSjekk::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.erDeltaker).isTrue()
        assertThat(response.body!!.fraOgMed).isEqualTo(LocalDate.of(2025, 1, 1))
        verify(exactly = 0) { sporingsloggService.logg(any(), any(), any(), any()) }
    }

    @Test
    fun `systemtoken - bruker er ikke deltaker`() {
        every { tilgangskontrollService.erSystemBruker() } returns true
        every { tilgangskontrollService.krevSystemtilgang(any()) } just runs
        every { registerService.sjekkAktivDeltakelse(deltakerIdent) } returns DeltakelseSjekk(erDeltaker = false)

        val response = testRestTemplate.exchange(
            "/ekstern/deltakelse/sjekk",
            HttpMethod.POST,
            HttpEntity(DeltakerDTO(deltakerIdent = deltakerIdent), azureSystemToken()),
            DeltakelseSjekk::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.erDeltaker).isFalse()
        verify(exactly = 0) { sporingsloggService.logg(any(), any(), any(), any()) }
    }

    @Test
    fun `systemtoken fra ikke-godkjent app - gir 403`() {
        every { tilgangskontrollService.erSystemBruker() } returns true
        every { tilgangskontrollService.krevSystemtilgang(any()) } throws
            ErrorResponseException(
                HttpStatus.FORBIDDEN,
                ProblemDetail.forStatusAndDetail(
                    HttpStatus.FORBIDDEN,
                    "Systemtjenesten er ikke tilgjengelig for innlogget bruker"
                ),
                null
            )

        val response = testRestTemplate.exchange(
            "/ekstern/deltakelse/sjekk",
            HttpMethod.POST,
            HttpEntity(DeltakerDTO(deltakerIdent = deltakerIdent), azureSystemToken()),
            String::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    // ── OBO-token fra godkjent system ──────────────────────────────────────────

    @Test
    fun `OBO-token veileder - bruker er aktiv deltaker og sporingslogg kalles`() {
        every { tilgangskontrollService.erSystemBruker() } returns false
        every { tilgangskontrollService.krevOboTilgangFraGodkjentEksternSystem(any(), any()) } just runs
        every { registerService.sjekkAktivDeltakelse(deltakerIdent) } returns DeltakelseSjekk(
            erDeltaker = true,
            fraOgMed = LocalDate.of(2025, 1, 1),
            tilOgMed = null
        )
        every { sporingsloggService.logg(any(), any(), any(), any()) } just runs

        val response = testRestTemplate.exchange(
            "/ekstern/deltakelse/sjekk",
            HttpMethod.POST,
            HttpEntity(DeltakerDTO(deltakerIdent = deltakerIdent), azureOboToken()),
            DeltakelseSjekk::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.erDeltaker).isTrue()
        assertThat(response.body!!.fraOgMed).isEqualTo(LocalDate.of(2025, 1, 1))
        assertThat(response.body!!.tilOgMed).isNull()
        verify(exactly = 1) { sporingsloggService.logg(any(), any(), any(), any()) }
    }

    @Test
    fun `OBO-token veileder - bruker er aktiv deltaker med fremtidig sluttdato`() {
        val fremtidigSluttdato = LocalDate.now().plusMonths(6)
        every { tilgangskontrollService.erSystemBruker() } returns false
        every { tilgangskontrollService.krevOboTilgangFraGodkjentEksternSystem(any(), any()) } just runs
        every { registerService.sjekkAktivDeltakelse(deltakerIdent) } returns DeltakelseSjekk(
            erDeltaker = true,
            fraOgMed = LocalDate.now().minusMonths(3),
            tilOgMed = fremtidigSluttdato
        )
        every { sporingsloggService.logg(any(), any(), any(), any()) } just runs

        val response = testRestTemplate.exchange(
            "/ekstern/deltakelse/sjekk",
            HttpMethod.POST,
            HttpEntity(DeltakerDTO(deltakerIdent = deltakerIdent), azureOboToken()),
            DeltakelseSjekk::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.erDeltaker).isTrue()
        assertThat(response.body!!.tilOgMed).isEqualTo(fremtidigSluttdato)
    }

    @Test
    fun `OBO-token veileder - bruker er ikke deltaker`() {
        every { tilgangskontrollService.erSystemBruker() } returns false
        every { tilgangskontrollService.krevOboTilgangFraGodkjentEksternSystem(any(), any()) } just runs
        every { registerService.sjekkAktivDeltakelse(deltakerIdent) } returns DeltakelseSjekk(erDeltaker = false)
        every { sporingsloggService.logg(any(), any(), any(), any()) } just runs

        val response = testRestTemplate.exchange(
            "/ekstern/deltakelse/sjekk",
            HttpMethod.POST,
            HttpEntity(DeltakerDTO(deltakerIdent = deltakerIdent), azureOboToken()),
            DeltakelseSjekk::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.erDeltaker).isFalse()
        assertThat(response.body!!.fraOgMed).isNull()
    }

    // ── tilgangsfeil ───────────────────────────────────────────────────────────

    @Test
    fun `OBO-token fra ikke-godkjent system - gir 403`() {
        every { tilgangskontrollService.erSystemBruker() } returns false
        every { tilgangskontrollService.krevOboTilgangFraGodkjentEksternSystem(any(), any()) } throws
            ErrorResponseException(
                HttpStatus.FORBIDDEN,
                ProblemDetail.forStatusAndDetail(
                    HttpStatus.FORBIDDEN,
                    "Kallet kommer fra et system som ikke har tilgang til dette endepunktet"
                ),
                null
            )

        val response = testRestTemplate.exchange(
            "/ekstern/deltakelse/sjekk",
            HttpMethod.POST,
            HttpEntity(DeltakerDTO(deltakerIdent = deltakerIdent), azureOboToken()),
            String::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `OBO-token veileder uten tilgang - gir 403`() {
        every { tilgangskontrollService.erSystemBruker() } returns false
        every { tilgangskontrollService.krevOboTilgangFraGodkjentEksternSystem(any(), any()) } throws
            ErrorResponseException(
                HttpStatus.FORBIDDEN,
                ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Ikke tilgang til kode6 person"),
                null
            )

        val response = testRestTemplate.exchange(
            "/ekstern/deltakelse/sjekk",
            HttpMethod.POST,
            HttpEntity(DeltakerDTO(deltakerIdent = deltakerIdent), azureOboToken()),
            String::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `mangler token - gir 401`() {
        val response = testRestTemplate.exchange(
            "/ekstern/deltakelse/sjekk",
            HttpMethod.POST,
            HttpEntity(
                DeltakerDTO(deltakerIdent = deltakerIdent),
                HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            ),
            String::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `ugyldig issuer - gir 401`() {
        val response = testRestTemplate.exchange(
            "/ekstern/deltakelse/sjekk",
            HttpMethod.POST,
            HttpEntity(
                DeltakerDTO(deltakerIdent = deltakerIdent),
                bearerHeaders(mockOAuth2Server.hentToken(issuerId = "ukjent"))
            ),
            String::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private fun azureSystemToken(): HttpHeaders =
        bearerHeaders(mockOAuth2Server.hentToken(issuerId = Issuers.AZURE, claims = mapOf("idtyp" to "app")))

    private fun azureOboToken(): HttpHeaders =
        bearerHeaders(mockOAuth2Server.hentToken(issuerId = Issuers.AZURE, claims = mapOf("NAVident" to "Z123456")))

    private fun bearerHeaders(token: SignedJWT) = HttpHeaders().apply {
        setBearerAuth(token.serialize())
        contentType = MediaType.APPLICATION_JSON
    }
}
