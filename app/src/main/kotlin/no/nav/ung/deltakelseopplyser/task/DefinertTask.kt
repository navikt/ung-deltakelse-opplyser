package no.nav.ung.deltakelseopplyser.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = DefinertTask.TYPE,
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 60L,
    beskrivelse = "Oppretter oppgave for julenissen",
)
class DefinertTask(
    private val taskService: TaskService
) : AsyncTaskStep {

    companion object {
        private val logger = LoggerFactory.getLogger(DefinertTask::class.java)
        const val TYPE = "Foo"
        fun opprettTask(data: String): Task {
            // payload er en streng, men kan også være en serialisert json,
            // som då kan plukkes opp i doTask
            return Task(type = TYPE, payload = data, properties = Properties())
                .medTriggerTid(LocalDateTime.now())
        }
    }

    override fun doTask(task: Task) {
        logger.info("Kjører task med id=${task.id} og payload=${task.payload}")
    }
}
