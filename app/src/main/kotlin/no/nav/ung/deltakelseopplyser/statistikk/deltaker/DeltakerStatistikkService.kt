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

    /**
     * Henter antall deltakere per oppgavetype.
     *
     * @return En liste over antall deltakere per oppgavetype.
     */
    fun antallDeltakerePerOppgavetype(): List<AntallDeltakerePerOppgavetypeRecord> {
        return deltakerStatistikkRepository.antallDeltakerePerOppgavetype().map {
            AntallDeltakerePerOppgavetypeRecord(
                oppgavetype = it.getOppgavetype(),
                oppgavestatus = it.getStatus(),
                antallDeltakere = it.getAntallDeltakere(),
                opprettetTidspunkt = ZonedDateTime.now()
            )
        }
    }
}
