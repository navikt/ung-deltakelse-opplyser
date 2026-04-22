package no.nav.ung.deltakelseopplyser

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.ung.deltakelseopplyser.statistikk.bigquery.BigQueryTestConfiguration
import no.nav.ung.deltakelseopplyser.utils.KafkaUtils.opprettKafkaConsumer
import no.nav.ung.deltakelseopplyser.utils.KafkaUtils.opprettKafkaProducer
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.producer.Producer
import org.junit.jupiter.api.AfterAll
import java.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import no.nav.ung.deltakelseopplyser.wiremock.AutoConfigureWireMock
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc

/**
 * Annotates a test class to run with embedded Kafka and Spring Boot.
 * The embedded Kafka will be started with 1 broker and 1 partition.
 * The topics specified in the annotation will be created.
 * The Spring property KAFKA_BROKERS will be set to the embedded Kafka broker address.
 * The test class will be started with the test profile.
 * The test class will be started with a random port.
 * The test class will be started with the SpringExtension.
 * The test class will be started with the TestInstance annotation.
 * The test class will be started with the EnableMockOAuth2Server annotation.
 * The test class will be started with the UngDeltakelseOpplyserApplication class.
 * The test class will be started with the SpringBootTest annotation.
 *
 * @see EmbeddedKafka
 * @see SpringBootTest
 * @see EnableMockOAuth2Server
 * @see TestInstance
 * @see SpringExtension
 * @see UngDeltakelseOpplyserApplication
 * @see ActiveProfiles
 */
@EmbeddedKafka(
    partitions = 1,
    count = 1,
    bootstrapServersProperty = "KAFKA_BROKERS",
    topics = [
        "dusseldorf.ungdomsytelse-soknad-cleanup",
        "dusseldorf.ungdomsytelse-oppgavebekreftelse-cleanup",
        "dusseldorf.ungdomsytelse-inntektsrapportering-cleanup",
        "k9saksbehandling.ung-vedtakhendelse",
        "min-side.aapen-brukervarsel-v1",
        "min-side.aapen-microfrontend-v1"
    ]
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension::class)
@EnableMockOAuth2Server
@ActiveProfiles("test")
@SpringBootTest(
    classes = [UngDeltakelseOpplyserApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
@AutoConfigureWireMock
@AutoConfigureTestRestTemplate
@Import(BigQueryTestConfiguration::class)
abstract class AbstractIntegrationTest {

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker // Broker som brukes til å konfigurere opp en kafka producer.

    @Autowired
    protected lateinit var mockOAuth2Server: MockOAuth2Server

    @Autowired
    lateinit var wireMockServer: WireMockServer

    protected lateinit var producer: Producer<String, Any> // Kafka producer som brukes til å legge på kafka meldinger. Mer spesifikk, Hendelser om pp-sykt-barn
    protected lateinit var consumer: Consumer<String, String> // Kafka producer som brukes til å legge på kafka meldinger. Mer spesifikk, Hendelser om pp-sykt-barn

    protected abstract val consumerGroupPrefix: String
    protected abstract val consumerGroupTopics: List<String>


    @BeforeAll
    fun setUp() {
        producer = embeddedKafkaBroker.opprettKafkaProducer(consumerGroupPrefix)
        consumer = embeddedKafkaBroker.opprettKafkaConsumer(
            groupPrefix = consumerGroupPrefix,
            topics = consumerGroupTopics
        )
    }

    @AfterAll
    fun tearDown() {
        producer.close(Duration.ZERO)
        consumer.close(Duration.ZERO)
    }
}
