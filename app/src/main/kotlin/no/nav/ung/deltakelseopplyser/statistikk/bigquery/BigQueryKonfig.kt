package no.nav.ung.deltakelseopplyser.statistikk.bigquery

import com.google.cloud.NoCredentials
import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile(value = ["vtp", "prod-gcp", "dev-gcp"])
class BigQueryKonfig {

    @Bean(name = ["bigQuery"])
    @Profile(value = ["prod-gcp", "dev-gcp"])
    fun bigQueryDefaultCredentials(): BigQuery {
        return BigQueryOptions.getDefaultInstance().service
    }

    @Bean(name = ["bigQuery"])
    @Profile(value = ["vtp"])
    fun bigQueryNoCredentials(): BigQuery {
        return BigQueryOptions.newBuilder()
            .setProjectId("test-project")
            .setCredentials(NoCredentials.getInstance())
            .build()
            .service
    }
}
