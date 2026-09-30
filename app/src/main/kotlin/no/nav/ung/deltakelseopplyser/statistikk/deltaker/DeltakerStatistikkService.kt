package no.nav.ung.deltakelseopplyser.statistikk.deltaker

import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class DeltakerStatistikkService(
    private val deltakerStatistikkRepository: DeltakerStatistikkRepository
) {
    fun antallDeltakereIUngdomsprogrammet(): AntallDeltakereIUngdomsprogrammetRecord {
        val antallDeltakere = deltakerStatistikkRepository.antallDeltakereIUngdomsprogrammet()
        return AntallDeltakereIUngdomsprogrammetRecord(
            antallDeltakere = antallDeltakere,
            opprettetTidspunkt = ZonedDateTime.now()
        )
    }
}
