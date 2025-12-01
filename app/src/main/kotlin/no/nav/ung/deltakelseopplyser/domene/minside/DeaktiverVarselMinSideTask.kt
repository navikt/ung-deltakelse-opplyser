package no.nav.ung.deltakelseopplyser.domene.minside

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
@TaskStepBeskrivelse(
    taskStepType = DeaktiverVarselMinSideTask.TYPE,
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 60L,
    beskrivelse = "Deaktiverer oppgave varsel på Min side",
)
class DeaktiverVarselMinSideTask private constructor(
    private val mineSiderService: MineSiderService,
) : AsyncTaskStep {

    companion object {
        private val logger = LoggerFactory.getLogger(DeaktiverVarselMinSideTask::class.java)
        const val TYPE = "deaktiver.varsel.min-side"

        fun opprettTask(oppgaveReferanse: UUID): Task {
            // payload er en streng, men kan også være en serialisert json,
            // som då kan plukkes opp i doTask
            return Task(
                type = TYPE,
                payload = oppgaveReferanse.toString(),
                properties = Properties()
            )
                .medTriggerTid(LocalDateTime.now())
        }
    }

    @WithSpan
    override fun doTask(task: Task) {
        logger.info("Kjører task med id=${task.id} og type=${task.type}")
        val oppgaveReferanse = task.payload
        mineSiderService.deaktiverOppgave(oppgaveReferanse)
    }
}
