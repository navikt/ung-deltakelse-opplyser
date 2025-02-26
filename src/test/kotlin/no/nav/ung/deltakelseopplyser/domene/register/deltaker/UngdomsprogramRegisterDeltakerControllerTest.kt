package no.nav.ung.deltakelseopplyser.domene.register.deltaker

import com.ninjasquad.springmockk.MockkBean
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.ung.deltakelseopplyser.ApiClient
import no.nav.ung.deltakelseopplyser.config.JacksonConfiguration
import no.nav.ung.deltakelseopplyser.domene.DeltakelseApi
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension::class)
@WebMvcTest(
    controllers = [UngdomsprogramRegisterDeltakerController::class]
)
@Import(
    JacksonConfiguration::class,
    UngdomsprogramregisterService::class,
    //UngdomsprogramRegisterDeltakerControllerTest.TestConfig::class
)
class UngdomsprogramRegisterDeltakerControllerTest {

    @LocalServerPort
    private var port: Int = 0

    @MockkBean
    private lateinit var registerService: UngdomsprogramregisterService

    @MockkBean
    private lateinit var springTokenValidationContextHolder: SpringTokenValidationContextHolder

    @Autowired
    private lateinit var apiClient: ApiClient

    @Test
    fun `Innsending av søknad er OK`() {

        DeltakelseApi(apiClient).hentAlleMineDeltakelser()

    }

    @TestConfiguration
    class TestConfig {

        @Bean
        fun api(): ApiClient {
            return ApiClient()
                .setBasePath("http://localhost:")
        }
    }
}
