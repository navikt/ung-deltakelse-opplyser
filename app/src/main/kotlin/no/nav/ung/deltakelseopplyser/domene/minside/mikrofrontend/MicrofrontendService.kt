package no.nav.ung.deltakelseopplyser.domene.minside.mikrofrontend

import no.nav.tms.microfrontend.Sensitivitet
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.minside.MineSiderService
import org.springframework.stereotype.Service

@Service
class MicrofrontendService(
    private val microfrontendRepository: MicrofrontendRepository,
    private val deltakerService: DeltakerService,
    private val mineSiderService: MineSiderService,
) {

    fun sendOgLagre(minSideMicrofrontendStatusDAO: MinSideMicrofrontendStatusDAO) {
        mineSiderService.aktiverMikrofrontend(
            deltakerIdent = minSideMicrofrontendStatusDAO.deltaker.deltakerIdent,
            microfrontendId = MicrofrontendId.UNGDOMSPROGRAMYTELSE_INNSYN,
            sensitivitet = Sensitivitet.HIGH,
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
