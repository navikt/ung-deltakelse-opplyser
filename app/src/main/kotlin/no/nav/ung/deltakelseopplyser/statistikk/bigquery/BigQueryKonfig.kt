package no.nav.ung.deltakelseopplyser.statistikk.bigquery

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryOptions
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.ung.deltakelseopplyser.http.MDCValuesPropagatingClientHttpRequestInterceptor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpRequest
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Configuration
class BigQueryKonfig {

    @Bean(name = ["bigQuery"])
    fun bigQuery(): BigQuery {
        return BigQueryOptions.getDefaultInstance().service
    }

}

