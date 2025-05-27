package no.nav.ung.deltakelseopplyser.domene.minside.mikrofrontend

import no.nav.tms.microfrontend.Sensitivitet
import no.nav.ung.deltakelseopplyser.domene.minside.MineSiderService
import org.springframework.stereotype.Service

@Service
class MikrofrontendService(
    private val mikrofrontendRepository: MikrofrontendRepository,
    private val mineSiderService: MineSiderService,
) {

    fun sendOgLagre(mikrofrontendDAO: MikrofrontendDAO) {
        mineSiderService.aktiverMikrofrontend(
            deltakerIdent = mikrofrontendDAO.deltaker.deltakerIdent,
            mikrofrontendId = MikrofrontendId.fraId(mikrofrontendDAO.mikrofrontendId),
            sensitivitet = Sensitivitet.HIGH,
        )
        mikrofrontendRepository.save(mikrofrontendDAO)
    }
}
