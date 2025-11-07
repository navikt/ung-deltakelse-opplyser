package no.nav.ung.deltakelseopplyser.domene.minside

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tms.microfrontend.Sensitivitet
import no.nav.ung.deltakelseopplyser.config.JacksonConfiguration
import no.nav.ung.deltakelseopplyser.domene.minside.mikrofrontend.MicrofrontendId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
@TaskStepBeskrivelse(
    taskStepType = AktiverMikrofrontendMinSideTask.TYPE,
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 60L,
    beskrivelse = "Aktiverer mikrofrontend på Min side",
)
class AktiverMikrofrontendMinSideTask private constructor(
    private val mineSiderService: MineSiderService,
) : AsyncTaskStep {

    companion object {
        private val logger = LoggerFactory.getLogger(AktiverMikrofrontendMinSideTask::class.java)
        const val TYPE = "aktiver.mikrofrontend.min-side"
        val mapper = jacksonObjectMapper().registerModule(JacksonConfiguration.JAVA_TIME_MODULE)

        fun opprettTask(aktiverMikrofrontendMinSideData: AktiverMikrofrontendMinSideData): Task {
            // payload er en streng, men kan også være en serialisert json,
            // som då kan plukkes opp i doTask
            return Task(
                type = TYPE,
                payload = mapper.writeValueAsString(aktiverMikrofrontendMinSideData),
                properties = Properties()
            )
                .medTriggerTid(LocalDateTime.now())
        }
    }

    override fun doTask(task: Task) {
        logger.info("Kjører task med id=${task.id} og type=${task.type}")
        val aktiverMikrofrontendMinSideData =
            mapper.readValue(task.payload, AktiverMikrofrontendMinSideData::class.java)

        mineSiderService.aktiverMikrofrontend(
            deltakerIdent = aktiverMikrofrontendMinSideData.deltakerIdent,
            microfrontendId = aktiverMikrofrontendMinSideData.microfrontendId,
            sensitivitet = aktiverMikrofrontendMinSideData.sensitivitet
        )
    }

    data class AktiverMikrofrontendMinSideData(
        val deltakerIdent: String,
        val microfrontendId: MicrofrontendId,
        val sensitivitet: Sensitivitet,
    )
}
