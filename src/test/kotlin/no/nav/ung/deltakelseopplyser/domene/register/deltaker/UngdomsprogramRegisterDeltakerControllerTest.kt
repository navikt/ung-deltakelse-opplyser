package no.nav.ung.deltakelseopplyser.domene.register.deltaker

import com.ninjasquad.springmockk.MockkBean
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.ung.deltakelseopplyser.ApiClient
import no.nav.ung.deltakelseopplyser.UngDeltakelseOpplyserApplication
import no.nav.ung.deltakelseopplyser.api.DeltakelseApi
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService
import no.nav.ung.deltakelseopplyser.utils.TokenTestUtils.hentToken
import no.nav.ung.deltakelseopplyser.utils.TokenTestUtils.mockContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension


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

    @MockkBean
    private lateinit var springTokenValidationContextHolder: SpringTokenValidationContextHolder

    @Test
    fun `Innsending av søknad er OK`() {
        springTokenValidationContextHolder.mockContext()

        DeltakelseApi(apiClient()).hentAlleMineDeltakelser()

    }

    fun apiClient(): ApiClient {
        return ApiClient(
            restTemplateBuilder
                .rootUri("http://localhost:$port")
                .additionalInterceptors(exchangeBearerTokenInterceptor())
                .build()
        )
    }

    fun exchangeBearerTokenInterceptor(): ClientHttpRequestInterceptor {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution ->
            val accessToken: String = mockOAuth2Server.hentToken().serialize()
            request.headers[HttpHeaders.AUTHORIZATION] = "Bearer $accessToken"
            execution.execute(request, body)
        }
    }
}
