package no.nav.ung.deltakelseopplyser.domene.register.historikk

import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseDAO
import org.springframework.data.repository.history.RevisionRepository
import java.util.*

interface DeltakelseHistorikkRepository : RevisionRepository<DeltakelseDAO, UUID, Long>
