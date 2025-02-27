package no.nav.ung.deltakelseopplyser.domene.register.deltaker

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.ung.deltakelseopplyser.ApiClient
import no.nav.ung.deltakelseopplyser.UngDeltakelseOpplyserApplication
import no.nav.ung.deltakelseopplyser.api.DeltakelseApi
import no.nav.ung.deltakelseopplyser.domene.oppgave.EndretStartdatoOppgavetypeDataDTO
import no.nav.ung.deltakelseopplyser.domene.oppgave.OppgaveDTO
import no.nav.ung.deltakelseopplyser.domene.oppgave.OppgaveStatus
import no.nav.ung.deltakelseopplyser.domene.oppgave.Oppgavetype
import no.nav.ung.deltakelseopplyser.domene.register.DeltakelsePeriodInfo
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService
import no.nav.ung.deltakelseopplyser.utils.TokenTestUtils.hentToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*


@DirtiesContext
@ExtendWith(SpringExtension::class)
@EnableMockOAuth2Server
@ActiveProfiles("test")
@SpringBootTest(
    classes = [UngDeltakelseOpplyserApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)

class UngdomsprogramRegisterDeltakerControllerTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplateBuilder: RestTemplateBuilder

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @MockkBean
    private lateinit var registerService: UngdomsprogramregisterService

    @Test
    fun `Innsending av søknad er OK`() {
        every { registerService.hentAlleDeltakelsePerioderForDeltaker(any()) } returns listOf(
            DeltakelsePeriodInfo(
                id = UUID.randomUUID(),
                fraOgMed = LocalDate.now(),
                tilOgMed = null,
                harSøkt = false,
                oppgaver = listOf(
                    OppgaveDTO(
                        id = UUID.randomUUID(),
                        oppgavetype = Oppgavetype.BEKREFT_ENDRET_STARTDATO,
                        oppgavetypeData = EndretStartdatoOppgavetypeDataDTO(
                            nyStartdato = LocalDate.now().plusWeeks(2),
                            veilederRef = "abc-123",
                            meldingFraVeileder = "Hei, du må møte opp på ny startdato"
                        ),
                        status = OppgaveStatus.ULØST,
                        opprettetDato = ZonedDateTime.now().minusDays(3),
                        løstDato = null
                    )
                ),
                rapporteringsPerioder = listOf()
            )
        )

        val alleMineDeltakelser = DeltakelseApi(apiClient()).hentAlleMineDeltakelser()
        assertThat(alleMineDeltakelser).hasSize(1)
    }

    fun apiClient(): ApiClient {
        val apiClient = ApiClient()
            .setBasePath("http://localhost:$port")

        apiClient.setBearerToken(mockOAuth2Server.hentToken().serialize())

        return apiClient
    }
}
