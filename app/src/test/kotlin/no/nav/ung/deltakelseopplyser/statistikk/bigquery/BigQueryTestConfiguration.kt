package no.nav.ung.deltakelseopplyser.statistikk.bigquery

import com.google.cloud.NoCredentials
import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.DatasetInfo
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.BigQueryEmulatorContainer
import org.testcontainers.junit.jupiter.Container

@TestConfiguration
class BigQueryTestConfiguration {

    companion object {
        const val BIG_QUERY_DATASET = "ung_sak_statistikk_dataset"
    }

    @Container
    val BIG_QUERY_EMULATOR_CONTAINER: BigQueryEmulatorContainer =
        BigQueryEmulatorContainer("ghcr.io/goccy/bigquery-emulator:0.4.3")

    @Bean(name = ["bigQuery"])
    fun bigQuery(): BigQuery {
        BIG_QUERY_EMULATOR_CONTAINER.start()
        val bigQuery = BigQueryOptions.newBuilder()
            .setProjectId("test-project")
            .setHost(BIG_QUERY_EMULATOR_CONTAINER.getEmulatorHttpEndpoint())
            .setCredentials(NoCredentials.getInstance())
            .build()
            .service
        bigQuery.create(DatasetInfo.newBuilder(BIG_QUERY_DATASET).build())
        return bigQuery;
    }
}