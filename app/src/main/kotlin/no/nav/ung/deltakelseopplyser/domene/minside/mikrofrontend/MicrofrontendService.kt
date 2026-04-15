package no.nav.ung.deltakelseopplyser.domene.minside.mikrofrontend

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tms.microfrontend.Sensitivitet
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.minside.MineSiderService
import no.nav.ung.deltakelseopplyser.domene.minside.AktiverMikrofrontendMinSideTask
import org.springframework.stereotype.Service

@Service
class MicrofrontendService internal constructor(
    private val microfrontendRepository: MicrofrontendRepository,
    private val deltakerService: DeltakerService,
    private val mineSiderService: MineSiderService,
    private val taskService: TaskService,
) {

    fun sendOgLagre(minSideMicrofrontendStatusDAO: MinSideMicrofrontendStatusDAO) {
        taskService.save(
            AktiverMikrofrontendMinSideTask.opprettTask(
                AktiverMikrofrontendMinSideTask.AktiverMikrofrontendMinSideData(
                    deltakerIdent = minSideMicrofrontendStatusDAO.deltaker.deltakerIdent,
                    microfrontendId = MicrofrontendId.UNGDOMSPROGRAMYTELSE_INNSYN,
                    sensitivitet = Sensitivitet.HIGH,
                )
            )
        )
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
