package no.nav.ung.deltakelseopplyser.domene.minside

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.ung.deltakelseopplyser.config.JacksonConfiguration
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettVarselMinSideTask.TYPE,
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 60L,
    beskrivelse = "Oppretter oppgave for å sende varsel til Min side",
)
class OpprettVarselMinSideTask private constructor(
    private val mineSiderService: MineSiderService,
) : AsyncTaskStep {

    companion object {
        private val logger = LoggerFactory.getLogger(OpprettVarselMinSideTask::class.java)
        const val TYPE = "opprett.varsel.min-side"
        val mapper = jacksonObjectMapper().registerModule(JacksonConfiguration.JAVA_TIME_MODULE)

        fun opprettTask(opprettVarselMinSideData: OpprettVarselMinSideData): Task {
            // payload er en streng, men kan også være en serialisert json,
            // som då kan plukkes opp i doTask
            return Task(
                type = TYPE,
                payload = mapper.writeValueAsString(opprettVarselMinSideData),
                properties = Properties()
            )
                .medTriggerTid(LocalDateTime.now())
        }
    }

    @WithSpan
    override fun doTask(task: Task) {
        logger.info("Kjører task med id=${task.id} og type=${task.type}")
        val opprettVarselMinSideData = mapper.readValue(task.payload, OpprettVarselMinSideData::class.java)
        mineSiderService.opprettVarsel(
            varselId = opprettVarselMinSideData.varselId,
            deltakerIdent = opprettVarselMinSideData.deltakerIdent,
            tekster = opprettVarselMinSideData.tekster,
            varselLink = opprettVarselMinSideData.varselLink,
            varseltype = opprettVarselMinSideData.varseltype,
            aktivFremTil = opprettVarselMinSideData.aktivFremTil
        )
    }

    data class OpprettVarselMinSideData(
        val varselId: String,
        val deltakerIdent: String,
        val tekster: List<Tekst>,
        val varselLink: String,
        val varseltype: Varseltype,
        val aktivFremTil: ZonedDateTime? = null,
    )
}
