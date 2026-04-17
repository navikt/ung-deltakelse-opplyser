package no.nav.ung.deltakelseopplyser.prosessering

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

object TestTaskTyper {
    const val FEIL_ETTER_INNER_REQUIRES_NEW = "transaksjons.test.feil.etter.inner.requires.new"
    const val ON_COMPLETION_FEIL = "transaksjons.test.on.completion.feil"
    const val ALLTID_OK = "transaksjons.test.alltid.ok"
    const val RETRY_MANUELL = "transaksjons.test.retry.manuell"
}

@Service
class TestTaskEffectsService(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun ensureTableExists() {
        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS test_task_effect (
                id BIGSERIAL PRIMARY KEY,
                effect_key VARCHAR NOT NULL,
                created_at TIMESTAMP DEFAULT LOCALTIMESTAMP
            )
            """.trimIndent()
        )
    }

    fun clear() {
        jdbcTemplate.update("DELETE FROM test_task_effect")
    }

    @Transactional
    fun insertCurrent(effectKey: String) {
        jdbcTemplate.update("INSERT INTO test_task_effect(effect_key) VALUES (?)", effectKey)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun insertRequiresNew(effectKey: String) {
        jdbcTemplate.update("INSERT INTO test_task_effect(effect_key) VALUES (?)", effectKey)
    }

    fun count(effectKey: String): Int =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM test_task_effect WHERE effect_key = ?",
            Int::class.java,
            effectKey
        ) ?: 0
}

@Service
@TaskStepBeskrivelse(
    taskStepType = TestTaskTyper.FEIL_ETTER_INNER_REQUIRES_NEW,
    maxAntallFeil = 3,
    triggerTidVedFeilISekunder = 1L,
    settTilManuellOppfølgning = false,
    beskrivelse = "Transaksjonstest: feiler etter indre requires_new",
)
class FeilEtterIndreRequiresNewTask(
    private val testTaskEffectsService: TestTaskEffectsService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        testTaskEffectsService.insertCurrent("current-${task.id}")
        testTaskEffectsService.insertRequiresNew("requires-new-${task.id}")
        error("Feiler med vilje i doTask")
    }
}

@Service
@TaskStepBeskrivelse(
    taskStepType = TestTaskTyper.ON_COMPLETION_FEIL,
    maxAntallFeil = 3,
    triggerTidVedFeilISekunder = 1L,
    settTilManuellOppfølgning = false,
    beskrivelse = "Transaksjonstest: feiler i onCompletion",
)
class OnCompletionFeilTask(
    private val testTaskEffectsService: TestTaskEffectsService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        testTaskEffectsService.insertCurrent("on-completion-${task.id}")
    }

    override fun onCompletion(task: Task) {
        error("Feiler med vilje i onCompletion")
    }
}

@Service
@TaskStepBeskrivelse(
    taskStepType = TestTaskTyper.ALLTID_OK,
    maxAntallFeil = 3,
    triggerTidVedFeilISekunder = 1L,
    settTilManuellOppfølgning = false,
    beskrivelse = "Transaksjonstest: alltid ok",
)
class AlltidOkTask(
    private val testTaskEffectsService: TestTaskEffectsService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        testTaskEffectsService.insertCurrent("ok-${task.id}")
    }
}

@Service
@TaskStepBeskrivelse(
    taskStepType = TestTaskTyper.RETRY_MANUELL,
    maxAntallFeil = 2,
    triggerTidVedFeilISekunder = 1L,
    settTilManuellOppfølgning = true,
    beskrivelse = "Transaksjonstest: retry og manuell oppfølging",
)
class RetryManuellTask : AsyncTaskStep {
    override fun doTask(task: Task) {
        error("Feiler alltid for å trigge retry")
    }
}
