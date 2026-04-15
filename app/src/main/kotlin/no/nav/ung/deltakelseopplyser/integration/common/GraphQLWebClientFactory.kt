package no.nav.ung.deltakelseopplyser.integration.common

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.ung.deltakelseopplyser.utils.Constants.NAV_CALL_ID
import no.nav.ung.deltakelseopplyser.utils.Constants.X_CORRELATION_ID
import no.nav.ung.deltakelseopplyser.utils.MDCUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
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

@Component
class GraphQLWebClientFactory(
    private val oauth2Config: ClientConfigurationProperties,
    private val oAuth2AccessTokenService: OAuth2AccessTokenService,
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(GraphQLWebClientFactory::class.java)
    }

    /**
     * Creates a configured GraphQLWebClient with common settings for retry, timeout, logging, and auth.
     *
     * @param url The GraphQL endpoint URL
     * @param oauth2ClientName The OAuth2 client registration name from configuration
     * @param additionalHeaders Optional map of additional headers to include in all requests
     * @return Configured GraphQLWebClient instance
     */
    fun createClient(
        url: String,
        oauth2ClientName: String,
        additionalHeaders: Map<String, String> = emptyMap()
    ): GraphQLWebClient {
        val oauth2ClientProperties = oauth2Config.registration[oauth2ClientName]
            ?: throw RuntimeException("could not find oauth2 client config for $oauth2ClientName")

        return GraphQLWebClient(
            url = url,
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
                .filter(createExchangeBearerTokenFilter(oauth2ClientProperties))
                .filter(createRequestLoggerInterceptor(logger))
                .filter(createRequestTracingInterceptor())
                .apply {
                    if (additionalHeaders.isNotEmpty()) {
                        it.defaultRequest { request ->
                            additionalHeaders.forEach { (key, value) ->
                                request.header(key, value)
                            }
                        }
                    }
                }
        )
    }


    private fun createExchangeBearerTokenFilter(
        oauth2ClientProperties: ClientProperties
    ) = ExchangeFilterFunction { request: ClientRequest, next: ExchangeFunction ->
        val accessToken: String = oAuth2AccessTokenService.getAccessToken(oauth2ClientProperties).access_token
            ?: throw IllegalStateException("Access token mangler")

        val filtered = ClientRequest.from(request)
            .headers {
                it.setBearerAuth(accessToken)
            }
            .build()

        next.exchange(filtered)
    }

    private fun createRequestLoggerInterceptor(logger: Logger) =
        ExchangeFilterFunction { request: ClientRequest, next: ExchangeFunction ->
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

    private fun createRequestTracingInterceptor() =
        ExchangeFilterFunction { clientRequest: ClientRequest, next: ExchangeFunction ->
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

