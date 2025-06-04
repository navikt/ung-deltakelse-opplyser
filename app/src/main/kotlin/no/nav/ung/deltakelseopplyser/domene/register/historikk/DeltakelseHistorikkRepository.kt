package no.nav.ung.deltakelseopplyser.domene.register.historikk

import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramDeltakelseDAO
import org.springframework.data.repository.history.RevisionRepository
import java.util.*

interface DeltakelseHistorikkRepository : RevisionRepository<UngdomsprogramDeltakelseDAO, UUID, Long>
