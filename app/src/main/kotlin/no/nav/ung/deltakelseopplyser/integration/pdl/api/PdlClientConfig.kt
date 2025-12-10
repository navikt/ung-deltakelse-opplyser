package no.nav.ung.deltakelseopplyser.integration.pdl.api

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.ung.deltakelseopplyser.utils.Constants.BEHANLINGSNUMMER
import no.nav.ung.deltakelseopplyser.utils.Constants.NAV_CALL_ID
import no.nav.ung.deltakelseopplyser.utils.Constants.X_CORRELATION_ID
import no.nav.ung.deltakelseopplyser.utils.MDCUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import reactor.netty.http.client.HttpClientRequest
import reactor.netty.http.client.HttpClientResponse
import reactor.util.retry.Retry
import java.time.Duration

@Configuration
class PdlClientConfig(
    @Value("\${no.nav.gateways.pdl-api-base-url}") private val pdlBaseUrl: String,
    oauth2Config: ClientConfigurationProperties,
    private val oAuth2AccessTokenService: OAuth2AccessTokenService,
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(PdlClientConfig::class.java)
    }

    private val azurePdlClientProperties = oauth2Config.registration["azure-pdl-api"]
        ?: throw RuntimeException("could not find oauth2 client config for azure-pdl-api")

    @Bean
    fun pdlClient(): GraphQLWebClient = GraphQLWebClient(
        url = "${pdlBaseUrl}/graphql",
        builder = WebClient.builder()
            .clientConnector(
                ReactorClientHttpConnector(
                    HttpClient.create()
                        .keepAlive(false)
                        .responseTimeout(Duration.ofSeconds(10))
                        .doOnRequest { request: HttpClientRequest, _ ->
                            logger.info("{} {} {}", request.version(), request.method(), request.resourceUrl())
                        }
                        .doOnResponse { response: HttpClientResponse, _ ->
                            logger.info(
                                "{} - {} {} {}",
                                response.status().toString(),
                                response.version(),
                                response.method(),
                                response.resourceUrl()
                            )
                        }
                )
            )
            .filter { request, next ->
                next.exchange(request)
                    .retryWhen(
                        Retry.max(3)
                            .filter { it is java.util.concurrent.TimeoutException }
                    )
            }
            .filter(exchangeBearerTokenFilter())
            .filter(requestLoggerInterceptor(logger))
            .filter(requestTracingInterceptor())
            .defaultRequest {
                it.header("Tema", "UNG")
                it.header(BEHANLINGSNUMMER, "B950")
            }
    )

    private fun exchangeBearerTokenFilter() = ExchangeFilterFunction { request: ClientRequest, next: ExchangeFunction ->
        val accessToken: String = oAuth2AccessTokenService.getAccessToken(azurePdlClientProperties).access_token
            ?: throw IllegalStateException("Access token mangler")

        val filtered = ClientRequest.from(request)
            .headers {
                it.setBearerAuth(accessToken)
            }
            .build()

        next.exchange(filtered)
    }

    fun requestLoggerInterceptor(logger: Logger) = ExchangeFilterFunction { request: ClientRequest, next: ExchangeFunction ->
            logger.info("HTTP Request: {} {}", request.method(), request.url())

            val response: Mono<ClientResponse> = next.exchange(request)
                .doOnNext { response ->
                    logger.info(
                        "HTTP Response: {} {} {}",
                        response.statusCode(),
                        request.method(),
                        request.url()
                    )
                }

            response
        }

    fun requestTracingInterceptor() = ExchangeFilterFunction { clientRequest: ClientRequest, next: ExchangeFunction ->
        val correlationId = MDCUtil.callIdOrNew()

        val filtered = ClientRequest.from(clientRequest)
            .headers { headers ->
                headers[NAV_CALL_ID] = correlationId
                headers[X_CORRELATION_ID] = correlationId
            }
            .build()

        next.exchange(filtered)
    }
}
