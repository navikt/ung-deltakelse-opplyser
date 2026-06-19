package no.nav.ung.deltakelseopplyser.domene.register.veileder

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.sif.abac.kontrakt.person.PersonIdent
import no.nav.ung.deltakelseopplyser.audit.SporingsloggService
import no.nav.ung.deltakelseopplyser.config.Issuers
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService
import no.nav.ung.deltakelseopplyser.domene.register.historikk.DeltakelseHistorikkService
import no.nav.ung.deltakelseopplyser.integration.abac.TilgangskontrollService
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseDTO
import no.nav.ung.deltakelseopplyser.statistikk.bigquery.BigQueryTestConfiguration
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
class UngdomsprogramRegisterVeilederControllerTest {

    @Autowired
    private lateinit var testRestTemplate: TestRestTemplate

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @MockkBean
    private lateinit var sporingsloggService: SporingsloggService

    @MockkBean
    private lateinit var tilgangskontrollService: TilgangskontrollService

    @MockkBean
    private lateinit var registerService: UngdomsprogramregisterService

    @MockkBean
    private lateinit var deltakelseHistorikkService: DeltakelseHistorikkService

    @MockkBean
    private lateinit var deltakerService: DeltakerService

    @Test
    fun `slett sluttdato returnerer 204`() {
        val deltakelseId = UUID.randomUUID()
        val deltakerIdent = "12345678910"
        val deltakelse = DeltakelseDTO(
            id = deltakelseId,
            deltaker = DeltakerDTO(deltakerIdent = deltakerIdent),
            fraOgMed = LocalDate.parse("2024-10-07"),
            tilOgMed = LocalDate.parse("2024-10-21"),
            periodeMaksDato = LocalDate.parse("2026-10-07"),
        )

        every { registerService.hentFraProgram(deltakelseId) } returns deltakelse
        every { tilgangskontrollService.krevAnsattTilgang(any(), any()) } just runs
        every { registerService.slettSluttdato(deltakelseId) } returns deltakelse.copy(tilOgMed = null)
        every { sporingsloggService.logg(any(), any(), any(), any()) } just runs

        val response = testRestTemplate.exchange(
            "/veileder/register/deltakelse/$deltakelseId/slett/sluttdato",
            HttpMethod.DELETE,
            HttpEntity<Void>(azureOboToken()),
            String::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        verify(exactly = 1) { registerService.slettSluttdato(deltakelseId) }
        verify(exactly = 1) {
            tilgangskontrollService.krevAnsattTilgang(any(), listOf(PersonIdent.fra(deltakerIdent)))
        }
    }

    @Test
    fun `slett sluttdato returnerer 404 nar deltakelse ikke finnes`() {
        val deltakelseId = UUID.randomUUID()

        every { registerService.hentFraProgram(deltakelseId) } throws ErrorResponseException(
            HttpStatus.NOT_FOUND,
            ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Fant ingen deltakelse med id $deltakelseId"),
            null
        )

        val response = testRestTemplate.exchange(
            "/veileder/register/deltakelse/$deltakelseId/slett/sluttdato",
            HttpMethod.DELETE,
            HttpEntity<Void>(azureOboToken()),
            String::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        verify(exactly = 0) { registerService.slettSluttdato(any()) }
    }

    private fun azureOboToken(): HttpHeaders = HttpHeaders().apply {
        setBearerAuth(
            mockOAuth2Server.hentToken(
                issuerId = Issuers.AZURE,
                claims = mapOf("NAVident" to "Z123456")
            ).serialize()
        )
        contentType = MediaType.APPLICATION_JSON
    }
}

