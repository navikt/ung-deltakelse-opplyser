package no.nav.ung.deltakelseopplyser.statistikk.bigquery

import com.ninjasquad.springmockk.MockkBean
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.ung.deltakelseopplyser.AbstractIntegrationTest
import no.nav.ung.deltakelseopplyser.statistikk.deltakelse.AntallDeltakelsePerEnhetStatistikkRecord
import no.nav.ung.deltakelseopplyser.statistikk.deltakelse.AntallDeltakelserPerEnhetTabell
import no.nav.ung.deltakelseopplyser.statistikk.deltaker.AntallDeltakereIUngdomsprogrammetRecord
import no.nav.ung.deltakelseopplyser.statistikk.deltaker.AntallDeltakereTabell
import no.nav.ung.deltakelseopplyser.utils.TokenTestUtils.mockContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.ZonedDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class BigQueryKlientTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var bigQueryKlient: BigQueryKlient

    @MockkBean
    private lateinit var springTokenValidationContextHolder: SpringTokenValidationContextHolder

    override val consumerGroupPrefix: String
        get() = "BigQueryKlientTest"
    override val consumerGroupTopics: List<String>
        get() = listOf()


    @BeforeEach
    fun beforeEach() {
        springTokenValidationContextHolder.mockContext()
    }


    @Test
    fun `Skal kunne publisere antall deltakere i programmet statistikk`() {
        val record = AntallDeltakereIUngdomsprogrammetRecord(10L, ZonedDateTime.now())
        bigQueryKlient.publish(BigQueryTestConfiguration.BIG_QUERY_DATASET, AntallDeltakereTabell, listOf(record))
    }

    @Test
    fun `Skal kunne publisere antall deltakelser per enhet statistikk`() {
        val records = listOf(
            AntallDeltakelsePerEnhetStatistikkRecord(
                kontor = "Kristiansand",
                antallDeltakelser = 15,
                opprettetTidspunkt = ZonedDateTime.now(),
                diagnostikk = emptyMap()
            ),
            AntallDeltakelsePerEnhetStatistikkRecord(
                kontor = "Oslo",
                antallDeltakelser = 10,
                opprettetTidspunkt = ZonedDateTime.now(),
                diagnostikk = emptyMap()
            ),
            AntallDeltakelsePerEnhetStatistikkRecord(
                kontor = "Arendal",
                antallDeltakelser = 5,
                opprettetTidspunkt = ZonedDateTime.now(),
                diagnostikk = emptyMap()
            ),
        )
        bigQueryKlient.publish(
            BigQueryTestConfiguration.BIG_QUERY_DATASET,
            AntallDeltakelserPerEnhetTabell,
            records
        )
    }
}
