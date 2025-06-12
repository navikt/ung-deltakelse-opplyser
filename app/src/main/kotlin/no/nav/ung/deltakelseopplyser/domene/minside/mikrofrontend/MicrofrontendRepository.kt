package no.nav.ung.deltakelseopplyser.domene.minside.mikrofrontend

import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDAO
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface MicrofrontendRepository : JpaRepository<MinSideMicrofrontendStatusDAO, UUID> {
    fun findByDeltaker(deltaker: DeltakerDAO): MinSideMicrofrontendStatusDAO?
}
