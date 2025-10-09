package no.nav.ung.deltakelseopplyser

import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.ung.deltakelseopplyser.statistikk.bigquery.BigQueryTestConfiguration
import no.nav.ung.deltakelseopplyser.task.DefinertTask
import no.nav.ung.deltakelseopplyser.task.KallTask
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.Import

@ActiveProfiles("test")
@EnableMockOAuth2Server
@AutoConfigureWireMock
@SpringBootTest
@Import(BigQueryTestConfiguration::class)
class UngDeltakelseOpplyserApplicationTests {

    @Autowired
    lateinit var kallTask: KallTask

    @Autowired
    lateinit var taskService: TaskService

    @Test
    fun contextLoads() {
        kallTask.opprettTask("test data")
        await.atMost(10, java.util.concurrent.TimeUnit.SECONDS).untilAsserted {
            val tasks = taskService.finnAlleTaskerMedType(DefinertTask.TYPE)
            assertThat(tasks).isNotEmpty
            val task = tasks.first()
            println(task.toString())
            assertThat(task.payload).isEqualTo("test data")
        }
    }
}
