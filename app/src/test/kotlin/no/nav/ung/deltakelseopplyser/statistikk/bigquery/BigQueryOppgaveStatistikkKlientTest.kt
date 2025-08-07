package no.nav.ung.deltakelseopplyser.statistikk.bigquery

import com.google.cloud.NoCredentials
import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.DatasetInfo
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
import no.nav.ung.deltakelseopplyser.statistikk.oppgave.OppgaveSvartidRecord
import no.nav.ung.deltakelseopplyser.statistikk.oppgave.OppgaveSvartidTabell
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.BigQueryEmulatorContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.ZonedDateTime


@Testcontainers
class BigQueryOppgaveStatistikkKlientTest {

    private lateinit var bigQueryKlient: BigQueryOppgaveStatistikkKlient

    @Container
    val BIG_QUERY_EMULATOR_CONTAINER: BigQueryEmulatorContainer =
        BigQueryEmulatorContainer("ghcr.io/goccy/bigquery-emulator:0.4.3")


    @BeforeEach
    fun setUp() {
        val bigQuery = BigQueryOptions.newBuilder()
            .setProjectId("test-project")
            .setHost(BIG_QUERY_EMULATOR_CONTAINER.getEmulatorHttpEndpoint())
            .setCredentials(NoCredentials.getInstance())
            .build()
            .service
        opprettDatasett(bigQuery = bigQuery)
        bigQueryKlient = BigQueryOppgaveStatistikkKlient(bigQuery)

    }


    private fun opprettDatasett(bigQuery: BigQuery) {
        bigQuery.create(DatasetInfo.newBuilder("ung_sak_statistikk_dataset").build())
    }

    @Test
    fun `Skal kunne publisere svartidstatistikk`() {
        val record = OppgaveSvartidRecord(1L, true, false, false, Oppgavetype.SØK_YTELSE, 100, ZonedDateTime.now())
        bigQueryKlient.publish(OppgaveSvartidTabell, listOf(record))

    }


}