package no.nav.ung.deltakelseopplyser

import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.ung.deltakelseopplyser.statistikk.bigquery.BigQueryTestConfiguration
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@EnableMockOAuth2Server
@AutoConfigureWireMock
@SpringBootTest
@Import(BigQueryTestConfiguration::class)
class UngDeltakelseOpplyserApplicationTests {

    @Test
    fun contextLoads() {
    }
}
