package no.nav.ung.deltakelseopplyser

import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@EnableMockOAuth2Server
@SpringBootTest
class UngDeltakelseOpplyserApplicationTests {

    @Test
    fun contextLoads() {
    }
}
