package no.nav.ung.deltakelseopplyser.statistikk.bigquery

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile(value = ["prod-gcp", "dev-gcp"])
class BigQueryKonfig {

    @Bean(name = ["bigQuery"])
    fun bigQuery(): BigQuery {
        return BigQueryOptions.getDefaultInstance().service
    }

}

