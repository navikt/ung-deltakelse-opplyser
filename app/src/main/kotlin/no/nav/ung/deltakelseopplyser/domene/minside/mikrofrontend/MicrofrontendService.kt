package no.nav.ung.deltakelseopplyser.domene.minside.mikrofrontend

import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tms.microfrontend.Sensitivitet
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.minside.MineSiderService
import no.nav.ung.deltakelseopplyser.domene.minside.task.AktiverMikrofrontendMinSideTask
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class MicrofrontendService internal constructor(
    private val microfrontendRepository: MicrofrontendRepository,
    private val deltakerService: DeltakerService,
    private val mineSiderService: MineSiderService,
    private val taskService: TaskService,
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(MicrofrontendService::class.java)

        // Statuser der en tilsvarende task fortsatt er "i flukt" (planlagt, plukket, eller under
        // behandling) - da skal vi ikke opprette en ny/duplikat task, den eksisterende vil kjøre.
        private val AKTIVE_TASK_STATUSER = setOf(
            Status.UBEHANDLET, Status.KLAR_TIL_PLUKK, Status.PLUKKET, Status.BEHANDLER
        )
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

        when (val eksisterendeTask = taskService.finnTaskMedPayloadOgType(payload, AktiverMikrofrontendMinSideTask.TYPE)) {
            null -> taskService.save(AktiverMikrofrontendMinSideTask.opprettTask(aktiverMikrofrontendMinSideData))

            else -> when (eksisterendeTask.status) {
                Status.FERDIG -> {
                    // Tasken er allerede kjørt ferdig tidligere. Siden payloaden er lik kan vi ikke
                    // opprette en ny rad - i stedet gjenbruker vi den samme raden ved å planlegge den
                    // på nytt, slik at aktiveringen faktisk publiseres til Min side igjen.
                    logger.info(
                        "Fant en tidligere fullført task for aktivering av mikrofrontend for deltaker med id={}. Planlegger den på nytt.",
                        deltaker.id
                    )
                    taskService.save(
                        eksisterendeTask.copy(status = Status.KLAR_TIL_PLUKK).medTriggerTid(LocalDateTime.now())
                    )
                }

                in AKTIVE_TASK_STATUSER -> logger.info(
                    "Task for aktivering av mikrofrontend finnes allerede (status={}) for deltaker med id={}. Hopper over oppretting av ny task.",
                    eksisterendeTask.status, deltaker.id
                )

                else -> logger.warn(
                    "Fant en task for aktivering av mikrofrontend for deltaker med id={} med status={}, som krever manuell oppfølging. Oppretter ikke ny task automatisk.",
                    deltaker.id, eksisterendeTask.status
                )
            }
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
