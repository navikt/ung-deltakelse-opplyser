package no.nav.ung.deltakelseopplyser.statistikk.bigquery

import com.google.cloud.NoCredentials
import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.DatasetInfo
import com.ninjasquad.springmockk.MockkBean
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
import no.nav.ung.deltakelseopplyser.statistikk.oppgave.OppgaveSvartidRecord
import no.nav.ung.deltakelseopplyser.statistikk.oppgave.OppgaveSvartidTabell
import no.nav.ung.deltakelseopplyser.utils.TokenTestUtils.mockContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.containers.BigQueryEmulatorContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.ZonedDateTime

@ActiveProfiles("test")
@EnableMockOAuth2Server
@AutoConfigureWireMock
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ExtendWith(SpringExtension::class)
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE
)
@Import(BigQueryTestConfiguration::class)
class BigQueryOppgaveStatistikkKlientTest {

    @Autowired
    private lateinit var bigQueryTestConfiguration: BigQueryTestConfiguration


    @MockkBean
    private lateinit var springTokenValidationContextHolder: SpringTokenValidationContextHolder

    private lateinit var bigQueryKlient: BigQueryOppgaveStatistikkKlient

    @BeforeEach
    fun setUp() {
        springTokenValidationContextHolder.mockContext()
        val bigQuery = bigQueryTestConfiguration.bigQuery()
        bigQueryKlient = BigQueryOppgaveStatistikkKlient(bigQuery)

    }


    @Test
    fun `Skal kunne publisere svartidstatistikk`() {
        val record = OppgaveSvartidRecord(1L, true, false, false, Oppgavetype.SØK_YTELSE, 100, ZonedDateTime.now())
        bigQueryKlient.publish(OppgaveSvartidTabell, listOf(record))

    }


}