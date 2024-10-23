package no.nav.ung.deltakelseopplyser

import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock

@ActiveProfiles("test")
@EnableMockOAuth2Server
@AutoConfigureWireMock
@SpringBootTest
class UngDeltakelseOpplyserApplicationTests {

    @Test
    fun contextLoads() {
    }
}
