package no.nav.ung.deltakelseopplyser.integration.ungsak

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
class UngOppgaverKlientKonfig(
    @Value("\${no.nav.gateways.ung-oppgaver}") private val ungOppgaverUrl: String,
    oauth2Config: ClientConfigurationProperties,
    private val oAuth2AccessTokenService: OAuth2AccessTokenService
) {

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(UngOppgaverKlientKonfig::class.java)

        const val AZURE_UNG_OPPGAVER = "azure-ung-oppgaver"
        const val TOKENX_UNG_OPPGAVER = "tokenx-ung-oppgaver"

    }

    private val azureUngOppgaverClientProperties =
        oauth2Config.registration[AZURE_UNG_OPPGAVER]
            ?: throw RuntimeException("could not find oauth2 client config for $AZURE_UNG_OPPGAVER")

    private val tokenXUngOppgaverClientProperties =
        oauth2Config.registration[TOKENX_UNG_OPPGAVER]
            ?: throw RuntimeException("could not find oauth2 client config for $TOKENX_UNG_OPPGAVER")

    @Bean(name = ["ungOppgaverKlient"])
    fun restTemplate(
        builder: RestTemplateBuilder,
        mdcInterceptor: MDCValuesPropagatingClientHttpRequestInterceptor,
    ): RestTemplate {
        return builder
            .connectTimeout(Duration.ofSeconds(20))
            .readTimeout(Duration.ofSeconds(20))
            .defaultHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .rootUri(ungOppgaverUrl)
            .defaultMessageConverters()
            .interceptors(bearerTokenInterceptor(), mdcInterceptor, requestLoggerInterceptor(logger))
            .build()
    }

    private fun bearerTokenInterceptor(): ClientHttpRequestInterceptor {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution ->
            when {
                request.uri.path == "/isalive" -> {} // ignorer

                else -> {
                    oAuth2AccessTokenService.getAccessToken(azureUngOppgaverClientProperties).access_token?.let {
                        request.headers.setBearerAuth(it)
                    }?: throw SecurityException("Access token er null")
                }
            }
            execution.execute(request, body)
        }
    }

    @Bean(name = ["ungOppgaverDeltakerKlient"])
    fun tokenXRestTemplate(
        builder: RestTemplateBuilder,
        mdcInterceptor: MDCValuesPropagatingClientHttpRequestInterceptor,
    ): RestTemplate {
        return builder
            .connectTimeout(Duration.ofSeconds(20))
            .readTimeout(Duration.ofSeconds(20))
            .defaultHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .rootUri(ungOppgaverUrl)
            .defaultMessageConverters()
            .interceptors(bearerTokenInterceptorForTokenX(), mdcInterceptor, requestLoggerInterceptor(logger))
            .build()
    }

    private fun bearerTokenInterceptorForTokenX(): ClientHttpRequestInterceptor {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution ->
            when {
                request.uri.path == "/isalive" -> {} // ignorer

                else -> {
                    oAuth2AccessTokenService.getAccessToken(tokenXUngOppgaverClientProperties).access_token?.let {
                        request.headers.setBearerAuth(it)
                    }?: throw SecurityException("Access token er null")
                }
            }
            execution.execute(request, body)
        }
    }

}

