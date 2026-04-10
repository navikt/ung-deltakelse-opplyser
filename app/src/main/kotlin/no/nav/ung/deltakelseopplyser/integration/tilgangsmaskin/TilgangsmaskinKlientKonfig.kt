package no.nav.ung.deltakelseopplyser.integration.tilgangsmaskin

import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.ung.deltakelseopplyser.http.MDCValuesPropagatingClientHttpRequestInterceptor
import no.nav.ung.deltakelseopplyser.utils.RestTemplateUtils.requestLoggerInterceptor
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
class TilgangsmaskinKlientKonfig(
    @Value("\${no.nav.gateways.tilgangsmaskin-base-url}") private val tilgangsmaskinBaseUrl: String,
    oauth2Config: ClientConfigurationProperties,
    private val oAuth2AccessTokenService: OAuth2AccessTokenService,
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(TilgangsmaskinKlientKonfig::class.java)

        const val AZURE_TILGANGSMASKIN = "azure-tilgangsmaskin"
    }

    private val azureTilgangsmaskinClientProperties =
        oauth2Config.registration[AZURE_TILGANGSMASKIN]
            ?: throw RuntimeException("could not find oauth2 client config for $AZURE_TILGANGSMASKIN")

    @Bean(name = ["tilgangsmaskinKlient"])
    fun restTemplate(
        builder: RestTemplateBuilder,
        mdcInterceptor: MDCValuesPropagatingClientHttpRequestInterceptor,
    ): RestTemplate {
        return builder
            .connectTimeout(Duration.ofSeconds(20))
            .readTimeout(Duration.ofSeconds(20))
            .defaultHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .rootUri(tilgangsmaskinBaseUrl)
            .defaultMessageConverters()
            .interceptors(bearerTokenInterceptor(), mdcInterceptor, requestLoggerInterceptor(logger))
            .build()
    }

    private fun bearerTokenInterceptor(): ClientHttpRequestInterceptor {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution ->
            oAuth2AccessTokenService.getAccessToken(azureTilgangsmaskinClientProperties).access_token?.let {
                request.headers.setBearerAuth(it)
            } ?: throw SecurityException("Access token er null")

            execution.execute(request, body)
        }
    }
}

