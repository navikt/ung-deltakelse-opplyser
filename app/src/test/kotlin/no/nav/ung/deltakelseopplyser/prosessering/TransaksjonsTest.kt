package no.nav.ung.deltakelseopplyser.prosessering

import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.error.RekjørSenereException
import no.nav.familie.prosessering.internal.TaskMaintenanceService
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.prosessering.internal.TaskStepExecutorService
import no.nav.familie.prosessering.internal.TaskWorker
import no.nav.ung.deltakelseopplyser.AbstractIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.annotation.Version
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Properties
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/**
 * Oppsummering av påstander:
 * 1) REQUIRES_NEW på TaskWorker-metoder + isolert feilhåndtering verifiseres
 * 2) `behandler -> doTask -> onCompletion -> ferdigstill` er atomisk
 * 3) Optimistisk låsing via `@Version` på `Task`
 * 4) `doActualWork` avbryter når task ikke er `PLUKKET`
 * 5) Graceful shutdown-mekanisme (`isShuttingDown`) finnes og stopper polling
 * 6) Retry/backoff og eskalering til `MANUELL_OPPFØLGING`
 * 7) Hengende `PLUKKET` task resettes til `KLAR_TIL_PLUKK`
 */
@ActiveProfiles("test")
class TransaksjonsTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var taskWorker: TaskWorker

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var taskMaintenanceService: TaskMaintenanceService

    @Autowired
    private lateinit var taskStepExecutorService: TaskStepExecutorService

    @Autowired
    private lateinit var jdbcAggregateTemplate: JdbcAggregateTemplate

    @Autowired
    private lateinit var testTaskEffectsService: TestTaskEffectsService

    @Autowired
    private lateinit var jdbcTemplate: org.springframework.jdbc.core.JdbcTemplate

    override val consumerGroupPrefix: String
        get() = "TransaksjonsTest"
    override val consumerGroupTopics: List<String>
        get() = emptyList()

    @BeforeEach
    fun setUpTransaksjonsTest() {
        testTaskEffectsService.ensureTableExists()
        testTaskEffectsService.clear()
    }

    @Test
    fun `påstand 1 - TaskWorker-metoder bruker REQUIRES_NEW og feilhåndtering lagres isolert`() {
        assertRequiresNew("markerPlukket", Long::class.javaPrimitiveType!!)
        assertRequiresNew("doActualWork", Long::class.javaPrimitiveType!!)
        assertRequiresNew("doFeilhåndtering", Long::class.javaPrimitiveType!!, Throwable::class.java)
        assertRequiresNew("rekjørSenere", Long::class.javaPrimitiveType!!, RekjørSenereException::class.java)

        val task = opprettTask(TestTaskTyper.FEIL_ETTER_INNER_REQUIRES_NEW)
        taskWorker.markerPlukket(task.id)
        val exception = assertThrows<IllegalStateException> { taskWorker.doActualWork(task.id) }

        assertThat(taskService.findById(task.id).status).isEqualTo(Status.PLUKKET)
        assertThat(testTaskEffectsService.count("current-${task.id}")).isZero()
        assertThat(testTaskEffectsService.count("requires-new-${task.id}")).isEqualTo(1)

        taskWorker.doFeilhåndtering(task.id, exception)

        val etterFeilhåndtering = taskService.findById(task.id)
        assertThat(etterFeilhåndtering.status).isEqualTo(Status.KLAR_TIL_PLUKK)
        assertThat(taskService.antallFeil(task.id)).isEqualTo(1)
    }

    @Test
    fun `påstand 2 - onCompletion-feil ruller tilbake doTask og task blir ikke FERDIG`() {
        val task = opprettTask(TestTaskTyper.ON_COMPLETION_FEIL)
        taskWorker.markerPlukket(task.id)

        assertThrows<IllegalStateException> { taskWorker.doActualWork(task.id) }

        val oppdatertTask = taskService.findById(task.id)
        assertThat(oppdatertTask.status).isEqualTo(Status.PLUKKET)
        assertThat(oppdatertTask.status).isNotEqualTo(Status.FERDIG)
        assertThat(testTaskEffectsService.count("on-completion-${task.id}")).isZero()
    }

    @Test
    fun `påstand 3 - Task har Version og stale oppdatering gir optimistic locking-feil`() {
        val versionField = Task::class.memberProperties.first { it.name == "versjon" }.javaField
        assertThat(versionField).isNotNull
        assertThat(versionField!!.isAnnotationPresent(Version::class.java)).isTrue()

        val task = opprettTask(TestTaskTyper.ALLTID_OK)
        val taskKopi1 = taskService.findById(task.id)
        val taskKopi2 = taskService.findById(task.id)

        jdbcAggregateTemplate.update(taskKopi1.medTriggerTid(taskKopi1.triggerTid.plusSeconds(5)))
        assertThrows<OptimisticLockingFailureException> {
            jdbcAggregateTemplate.update(taskKopi2.medTriggerTid(taskKopi2.triggerTid.plusSeconds(10)))
        }
    }

    @Test
    fun `påstand 4 - doActualWork hopper over task som ikke er PLUKKET`() {
        val task = opprettTask(TestTaskTyper.ALLTID_OK)
        assertThat(taskService.findById(task.id).status).isEqualTo(Status.UBEHANDLET)

        taskWorker.doActualWork(task.id)

        val oppdatertTask = taskService.findById(task.id)
        assertThat(oppdatertTask.status).isEqualTo(Status.UBEHANDLET)
        assertThat(testTaskEffectsService.count("ok-${task.id}")).isZero()
    }

    @Test
    fun `påstand 5 - graceful shutdown-flagg finnes og stopper ny polling`() {
        val targetClass = AopUtils.getTargetClass(taskStepExecutorService)
        assertThat(ApplicationListener::class.java.isAssignableFrom(targetClass)).isTrue()

        val isShuttingDownField = targetClass.getDeclaredField("isShuttingDown")
        isShuttingDownField.isAccessible = true
        val original = isShuttingDownField.getBoolean(taskStepExecutorService)

        val task = opprettTask(TestTaskTyper.ALLTID_OK)
        try {
            isShuttingDownField.setBoolean(taskStepExecutorService, true)
            taskStepExecutorService.pollAndExecute()
        } finally {
            isShuttingDownField.setBoolean(taskStepExecutorService, original)
        }

        val oppdatertTask = taskService.findById(task.id)
        assertThat(oppdatertTask.status).isEqualTo(Status.UBEHANDLET)
        assertThat(testTaskEffectsService.count("ok-${task.id}")).isZero()
    }

    @Test
    fun `påstand 6 - retry med backoff eskalerer til MANUELL_OPPFØLGING etter maks feil`() {
        val task = opprettTask(TestTaskTyper.RETRY_MANUELL)

        var forrigeTriggerTid = task.triggerTid
        repeat(2) { attempt ->
            await.atMost(10, TimeUnit.SECONDS).until {
                taskService.findById(task.id).kanPlukkes()
            }

            taskWorker.markerPlukket(task.id)
            val exception = assertThrows<IllegalStateException> { taskWorker.doActualWork(task.id) }
            taskWorker.doFeilhåndtering(task.id, exception)

            val oppdatertTask = taskService.findById(task.id)
            if (attempt == 0) {
                assertThat(oppdatertTask.status).isEqualTo(Status.KLAR_TIL_PLUKK)
                assertThat(oppdatertTask.triggerTid).isAfter(forrigeTriggerTid)
                forrigeTriggerTid = oppdatertTask.triggerTid
            }
        }

        val sluttstatus = taskService.findById(task.id)
        assertThat(sluttstatus.status).isEqualTo(Status.MANUELL_OPPFØLGING)
        assertThat(taskService.antallFeil(task.id)).isGreaterThanOrEqualTo(2)
    }

    @Test
    fun `påstand 7 - PLUKKET task eldre enn en time resettes til KLAR_TIL_PLUKK`() {
        val task = opprettTask(TestTaskTyper.ALLTID_OK)
        taskWorker.markerPlukket(task.id)
        assertThat(taskService.findById(task.id).status).isEqualTo(Status.PLUKKET)

        jdbcTemplate.update(
            "UPDATE task_logg SET opprettet_tid = now() - interval '2 hours' WHERE task_id = ? AND type = 'PLUKKET'",
            task.id
        )

        taskMaintenanceService.settPermanentPlukketTilKlarTilPlukk()

        val oppdatertTask = taskService.findById(task.id)
        assertThat(oppdatertTask.status).isEqualTo(Status.KLAR_TIL_PLUKK)
    }

    private fun opprettTask(type: String): Task =
        taskService.save(
            Task(
                type = type,
                payload = UUID.randomUUID().toString(),
                properties = Properties()
            ).medTriggerTid(LocalDateTime.now().minus(2, ChronoUnit.SECONDS))
        )

    private fun assertRequiresNew(methodName: String, vararg parameterTypes: Class<*>) {
        val targetClass = AopUtils.getTargetClass(taskWorker)
        val method = targetClass.getMethod(methodName, *parameterTypes)
        val annotation = AnnotatedElementUtils.findMergedAnnotation(method, Transactional::class.java)

        assertThat(annotation).isNotNull
        assertThat(annotation!!.propagation).isEqualTo(Propagation.REQUIRES_NEW)
    }
}
