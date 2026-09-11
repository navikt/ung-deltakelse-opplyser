package no.nav.ung.deltakelseopplyser.domene.minside.mikrofrontend

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tms.microfrontend.Sensitivitet
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.minside.MineSiderService
import no.nav.ung.deltakelseopplyser.domene.minside.task.AktiverMikrofrontendMinSideTask
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class MicrofrontendService internal constructor(
    private val microfrontendRepository: MicrofrontendRepository,
    private val deltakerService: DeltakerService,
    private val mineSiderService: MineSiderService,
    private val taskService: TaskService,
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(MicrofrontendService::class.java)
    }

    fun sendOgLagre(minSideMicrofrontendStatusDAO: MinSideMicrofrontendStatusDAO) {
        val deltaker = minSideMicrofrontendStatusDAO.deltaker
        val eksisterendeStatus = microfrontendRepository.findByDeltaker(deltaker)
        if (eksisterendeStatus?.status == MicrofrontendStatus.ENABLE) {
            logger.info(
                "Mikrofrontend er allerede aktivert for deltaker med id={}. Hopper over ny aktivering.",
                deltaker.id
            )
            return
        }

        val aktiverMikrofrontendMinSideData = AktiverMikrofrontendMinSideTask.AktiverMikrofrontendMinSideData(
            deltakerIdent = deltaker.deltakerIdent,
            microfrontendId = MicrofrontendId.UNGDOMSPROGRAMYTELSE_INNSYN,
            sensitivitet = Sensitivitet.HIGH,
        )
        val payload = AktiverMikrofrontendMinSideTask.mapper.writeValueAsString(aktiverMikrofrontendMinSideData)
        val eksisterendeTask = taskService.finnTaskMedPayloadOgType(payload, AktiverMikrofrontendMinSideTask.TYPE)
        if (eksisterendeTask == null) {
            taskService.save(
                AktiverMikrofrontendMinSideTask.opprettTask(aktiverMikrofrontendMinSideData)
            )
        } else {
            logger.info(
                "Task for aktivering av mikrofrontend finnes allerede for deltaker med id={}. Hopper over oppretting av ny task.",
                deltaker.id
            )
        }

        microfrontendRepository.save(minSideMicrofrontendStatusDAO)
    }

    fun deaktiver(eksisterende: MinSideMicrofrontendStatusDAO) {
        mineSiderService.deaktiverMikrofrontend(
            deltakerIdent = eksisterende.deltaker.deltakerIdent,
            microfrontendId = MicrofrontendId.UNGDOMSPROGRAMYTELSE_INNSYN
        )

        eksisterende.settStatus(MicrofrontendStatus.DISABLE)
        deltakerService.oppdaterDeltaker(eksisterende.deltaker);

    }


}
