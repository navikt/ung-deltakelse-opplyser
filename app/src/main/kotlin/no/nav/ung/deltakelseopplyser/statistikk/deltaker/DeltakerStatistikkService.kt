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
     * Henter aggregeringen av oppgaveantall og antall deltakere.
     * Grupperer deltakere etter antall oppgaver.
     *
     * @return En liste av AntallOppgaverAntallDeltakereFordeling som inneholder aggregeringen.
     */
    fun antallDeltakereEtterAntallOppgaverFordeling(): List<AntallDeltakereAntallOppgaverFordelingRecord> {
        return deltakerStatistikkRepository.antallDeltakereEtterAntallOppgaverFordeling().map {
            AntallDeltakereAntallOppgaverFordelingRecord(
                antallDeltakere = it.getAntallDeltakere(),
                antallOppgaver = it.getAntallOppgaver(),
                opprettetTidspunkt = ZonedDateTime.now()
            )
        }
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
