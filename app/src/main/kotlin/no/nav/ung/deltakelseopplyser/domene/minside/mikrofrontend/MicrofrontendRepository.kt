package no.nav.ung.deltakelseopplyser.domene.minside.mikrofrontend

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface MicrofrontendRepository : JpaRepository<MinSideMicrofrontendStatusDAO, UUID>
