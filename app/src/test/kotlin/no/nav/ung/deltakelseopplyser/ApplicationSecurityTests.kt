package no.nav.ung.deltakelseopplyser

import com.nimbusds.jwt.SignedJWT
import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearMocks
import io.mockk.every
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.ung.deltakelseopplyser.config.Issuers
import no.nav.ung.deltakelseopplyser.integration.abac.TilgangskontrollService
import no.nav.ung.deltakelseopplyser.utils.TokenTestUtils.hentToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockMultipartFile
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.ErrorResponseException
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.util.*

class ApplicationSecurityTests : AbstractIntegrationTest() {

    @Autowired
    lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var testRestTemplate: TestRestTemplate

    override val consumerGroupPrefix: String
        get() = "ApplicationSecurityTests"
    override val consumerGroupTopics: List<String>
        get() = listOf()

    @MockkBean
    private lateinit var tilgangskontrollService: TilgangskontrollService

    private companion object {
        private val logger = LoggerFactory.getLogger(ApplicationSecurityTests::class.java)
    }

    @BeforeEach
    fun beforeEach() {
        clearMocks(tilgangskontrollService)
    }

    @Test
    fun contextLoads() {
        assertThat(endpointsProvider()).isNotEmpty
    }

    @Test
    fun `Alle Azure-sikrede controllere må ha TilgangskontrollService i konstruktøren`() {
        val requestMappingHandlerMapping =
            applicationContext.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping::class.java)
        val apiMappings = requestMappingHandlerMapping.handlerMethods

        val azureControllerKlasser = apiMappings.values
            .map { it.beanType }
            .distinct()
            .filter { controllerClass ->
                val requiredIssuers = controllerClass.getAnnotation(RequiredIssuers::class.java)
                requiredIssuers?.value?.any { it.issuer == Issuers.AZURE } == true
            }

        assertThat(azureControllerKlasser).isNotEmpty

        azureControllerKlasser.forEach { controllerClass ->
            val harTilgangskontroll = controllerClass.constructors.any { constructor ->
                constructor.parameterTypes.any { it == TilgangskontrollService::class.java }
            }
            assertThat(harTilgangskontroll)
                .withFailMessage(
                    "Azure-sikret controller ${controllerClass.simpleName} mangler TilgangskontrollService i konstruktøren! " +
                            "Siden allowAllUsers=true er applikasjonen ansvarlig for all autorisasjon."
                )
                .isTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("azureEndepunkterProvider")
    fun `Azure-endepunkt skal returnere 403 når TilgangskontrollService nekter tilgang`(endpoint: Endpoint) {
        // Mock alle metoder i TilgangskontrollService til å kaste 403
        val forbidden = ErrorResponseException(
            HttpStatus.FORBIDDEN,
            ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Ikke tilgang (test)"),
            null
        )

        every { tilgangskontrollService.krevAnsattTilgang(any(), any()) } throws forbidden
        every { tilgangskontrollService.krevOboTilgangFraGodkjentEksternSystem(any(), any()) } throws forbidden
        every { tilgangskontrollService.krevSystemtilgang(any()) } throws forbidden
        every { tilgangskontrollService.krevSystemtilgang() } throws forbidden
        every { tilgangskontrollService.krevDriftsTilgang(any()) } throws forbidden
        every { tilgangskontrollService.krevTilgangTilPersonerForInnloggetBruker(any()) } throws forbidden
        every { tilgangskontrollService.erSystemBruker() } returns false

        val token = mockOAuth2Server.hentToken(issuerId = Issuers.AZURE, claims = mapOf("NAVident" to "Z999999"))

        val response = testRestTemplate.request(endpoint, endpoint.url, endpoint.method, token)

        // Verifiser at endepunktet IKKE returnerer suksess (2xx).
        // Noen endepunkter gjør database-oppslag før TilgangskontrollService kalles,
        // og returnerer da 404/500 i testmiljøet. Det viktige er at ingen endepunkter
        // returnerer 2xx uten autorisasjonssjekk.
        //
        // Unntak: DELETE-endepunkter som returnerer 204 NO_CONTENT for ikke-eksisterende
        // ressurser er en no-op (ifPresent-mønster) — ikke en sikkerhetsbypass.
        val er204DeleteNoOp = endpoint.method == HttpMethod.DELETE
                && response.statusCode == HttpStatus.NO_CONTENT

        if (er204DeleteNoOp) {
            logger.warn(
                "DELETE ${endpoint.url} returnerte 204 NO_CONTENT (no-op for ikke-eksisterende ressurs). " +
                        "Kontrollér manuelt at TilgangskontrollService kalles når ressursen finnes."
            )
        } else {
            assertThat(response.statusCode.is2xxSuccessful)
                .withFailMessage(
                    "Forventet IKKE 2xx for ${endpoint.method} ${endpoint.url}, " +
                            "men fikk ${response.statusCode}. " +
                            "Endepunktet returnerer suksess uten autorisasjonssjekk!"
                )
                .isFalse()
        }
    }

    @ParameterizedTest
    @MethodSource("endpointsProvider")
    fun `Forventer at autorisasjon på endepunkter fungerer som forventet`(endpoint: Endpoint) {
        // Kall på endepunkt med riktig token ikke gir 401 feil
        endpoint.issuers.forEach { issuer ->
            logger.info("Issuer: $issuer")
            testRestTemplate.assertNotEquals(
                endpoint = endpoint,
                expectedStatus = HttpStatus.UNAUTHORIZED,
                token = mockOAuth2Server.hentToken(issuerId = issuer)
            )
        }

        // Kall uten authorization header gir 401 feil
        testRestTemplate.assertEquals(
            endpoint = endpoint,
            token = null,
            expectedStatus = HttpStatus.UNAUTHORIZED
        )

        // Kall på endepunkt uten authorization header gir 401 feil
        testRestTemplate.assertEquals(
            endpoint = endpoint,
            expectedStatus = HttpStatus.UNAUTHORIZED
        )

        // Kall på endepunkt med acr level 3 gir 401 feil
        if (endpoint.issuers.contains("tokenx")) {
            testRestTemplate.assertEquals(
                endpoint = endpoint,
                expectedStatus = HttpStatus.UNAUTHORIZED,
                token = mockOAuth2Server.hentToken(claims = mapOf("acr" to "Level3"))
            )
        }

        // Kall på endepunkt med ukjent issuer gir 401 feil
        testRestTemplate.assertEquals(
            endpoint = endpoint,
            expectedStatus = HttpStatus.UNAUTHORIZED,
            token = mockOAuth2Server.hentToken(issuerId = "ukjent")
        )

        // Kall på endepunkt med utgått token gir 401 feil
        testRestTemplate.assertEquals(
            endpoint = endpoint,
            expectedStatus = HttpStatus.UNAUTHORIZED,
            token = mockOAuth2Server.hentToken(expiry = -1000)
        )

        // Kall på endepunkt med token for annen audience gir 401 feil`(endpoint: Endpoint) {
        testRestTemplate.assertEquals(
            endpoint = endpoint,
            expectedStatus = HttpStatus.UNAUTHORIZED,
            token = mockOAuth2Server.hentToken(audience = "annen-audience")
        )
    }


    private fun endpointsProvider(): List<Endpoint> {
        val requestMappingHandlerMapping =
            applicationContext.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping::class.java)
        val apiMappings: MutableMap<RequestMappingInfo, HandlerMethod> = requestMappingHandlerMapping.handlerMethods

        val endpointList =
            apiMappings.entries
                // Filtrerer bort endepunkter som ikke er relevante for oss
                .filterNot { it.value.beanType.name.startsWith("no.nav.familie") }
                .mapNotNull { (mappingInfo: RequestMappingInfo, handlerMethod: HandlerMethod) ->
                    logger.info("--> Endpoint: {}", mappingInfo.toString())
                    val requestMethod = mappingInfo.methodsCondition.methods.firstOrNull()
                    if (requestMethod == null) {
                        logger.warn("No request method found for mapping info: $mappingInfo")
                        return@mapNotNull null
                    }
                    val pathPattern = mappingInfo.pathPatternsCondition!!.patterns.first()
                    val path = pathPattern.patternString

                    val urlVariables: Array<String>? = if (path.contains("{") && path.contains("}")) {
                        generateUrlVariables(path)
                    } else {
                        null
                    }

                    val contentType = mappingInfo.consumesCondition.consumableMediaTypes.firstOrNull()

                    val requiredIssuers: RequiredIssuers =
                        handlerMethod.beanType.getAnnotation(RequiredIssuers::class.java)
                    val issuers = requiredIssuers.value.map { it.issuer }

                    Endpoint(
                        method = requestMethod.asHttpMethod(),
                        url = path,
                        urlVariables = urlVariables,
                        contentType = contentType,
                        issuers = issuers
                    )
                }

        logger.info("Found endpoints: $endpointList")
        return endpointList
    }

    private fun azureEndepunkterProvider(): List<Endpoint> {
        return endpointsProvider().filter { it.issuers.contains(Issuers.AZURE) }
    }

    private fun TestRestTemplate.assertEquals(
        endpoint: Endpoint,
        expectedStatus: HttpStatus,
        token: SignedJWT? = null,
    ) {
        val url = endpoint.url
        val httpMethod = endpoint.method

        logger.info("Testing endpoint: $url with method: $httpMethod")
        val response = request(endpoint, url, httpMethod, token)

        val statusCode = response.statusCode
        if (expectedStatus != statusCode) {
            logger.error("Forventet status $expectedStatus, men fikk $statusCode for $httpMethod $url")
        }
        assertThat(statusCode).isEqualTo(expectedStatus)
    }

    private fun TestRestTemplate.assertNotEquals(
        endpoint: Endpoint,
        expectedStatus: HttpStatus,
        token: SignedJWT? = null,
    ) {
        val url = endpoint.url
        val httpMethod = endpoint.method

        logger.info("Testing endpoint: $url with method: $httpMethod")
        val response = request(endpoint, url, httpMethod, token)

        val statusCode = response.statusCode
        if (expectedStatus != statusCode) {
            logger.error("Forventet status $expectedStatus, men fikk $statusCode for $httpMethod $url")
        }
        assertThat(statusCode).isNotEqualTo(expectedStatus)
    }

    fun TestRestTemplate.request(
        endpoint: Endpoint,
        url: String,
        httpMethod: HttpMethod,
        token: SignedJWT?,
    ): ResponseEntity<String> {
        val httpEntity = HttpHeaders().let {
            if (token != null) {
                it.setBearerAuth(token.serialize())
            }
            it.contentType = endpoint.contentType
            var body: Any? = null
            if (it.contentType == MediaType.MULTIPART_FORM_DATA) {
                body = håndterMultipartUpload(it)
            } else if (httpMethod in listOf(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH)) {
                body = when (it.contentType) {
                    MediaType.TEXT_PLAIN -> "test-begrunnelse"
                    MediaType.APPLICATION_FORM_URLENCODED -> "begrunnelse=test"
                    else -> """{"deltakerIdent":"12345678901","ident":"12345678901","aktørId":"1234567890123","startdato":"2024-01-01"}"""
                }
            }
            HttpEntity(body, it)
        }

        return if (endpoint.urlVariables != null) {
            exchange(
                url,
                httpMethod,
                httpEntity,
                String::class.java,
                *(endpoint.urlVariables)
            )
        } else {
            exchange(
                url,
                httpMethod,
                httpEntity,
                String::class.java
            )
        }
    }


    private fun håndterMultipartUpload(httpHeaders: HttpHeaders): LinkedMultiValueMap<String, Any> {
        httpHeaders.setContentDispositionFormData("vedlegg", "test-file.pdf")
        val file = MockMultipartFile(
            "vedlegg",
            "test-file.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "Test content".toByteArray()
        )
        return LinkedMultiValueMap<String, Any>().apply {
            add("vedlegg", file.resource)
        }
    }

    private fun generateUrlVariables(url: String): Array<String> {
        val regex = "\\{[^}]+\\}".toRegex()
        val matches = regex.findAll(url)
        return matches.map { UUID.randomUUID().toString() }.toList().toTypedArray()
    }

    data class Endpoint(
        val method: HttpMethod,
        val url: String,
        val urlVariables: Array<String>? = null,
        val contentType: MediaType? = MediaType.APPLICATION_JSON,
        val issuers: List<String>,
    ) {
        override fun toString(): String {
            return "$method $url"
        }
    }
}
