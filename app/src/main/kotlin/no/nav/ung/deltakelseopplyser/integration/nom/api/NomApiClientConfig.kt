package no.nav.ung.deltakelseopplyser.integration.nom.api

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
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

@Configuration
class NomApiClientConfig(
    @Value("\${no.nav.gateways.nom-api-base-url}") private val nomApiBaseUrl: String,
    oauth2Config: ClientConfigurationProperties,
    private val oAuth2AccessTokenService: OAuth2AccessTokenService,
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(NomApiClientConfig::class.java)
    }

    private val azureNomApiClientProperties = oauth2Config.registration["azure-nom-api"]
        ?: throw RuntimeException("could not find oauth2 client config for azure-nom-api")

    @Bean
    fun nomApiClient(): GraphQLWebClient = GraphQLWebClient(
        url = "${nomApiBaseUrl}/graphql",
        builder = WebClient.builder()
            .clientConnector(
                ReactorClientHttpConnector(
                    HttpClient.create()
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
            .filter(exchangeBearerTokenFilter())
            .filter(requestLoggerInterceptor(logger))
            .filter(requestTracingInterceptor())
    )

    private fun exchangeBearerTokenFilter() = ExchangeFilterFunction { request: ClientRequest, next: ExchangeFunction ->
        val accessToken: String = oAuth2AccessTokenService.getAccessToken(azureNomApiClientProperties).access_token
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
