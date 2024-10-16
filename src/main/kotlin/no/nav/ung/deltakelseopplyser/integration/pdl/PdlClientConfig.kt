package no.nav.ung.deltakelseopplyser.integration.pdl

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
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import reactor.netty.http.client.HttpClientRequest
import reactor.netty.http.client.HttpClientResponse

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
            .defaultRequest {
                it.header("Tema", "OMS")
                it.header(BEHANLINGSNUMMER, "B249") // TODO: Bytt ut med riktig behandlingsnummer før lansering
            }
    )

    private fun exchangeBearerTokenFilter() = { request: ClientRequest, next: ExchangeFunction ->
        val accessToken: String = oAuth2AccessTokenService.getAccessToken(azurePdlClientProperties).accessToken
            ?: throw IllegalStateException("Access token mangler")

        request.headers().setBearerAuth(accessToken)
        next.exchange(request)
    }

    fun requestLoggerInterceptor(logger: Logger) = { request: ClientRequest, next: ExchangeFunction ->
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

    fun requestTracingInterceptor() = { request: ClientRequest, next: ExchangeFunction ->
        val correlationId = MDCUtil.callIdOrNew()
        request.headers().computeIfAbsent(NAV_CALL_ID) { listOf(correlationId) }
        request.headers().computeIfAbsent(X_CORRELATION_ID) { listOf(correlationId) }

        next.exchange(request)
    }
}
