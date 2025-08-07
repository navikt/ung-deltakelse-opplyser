package no.nav.ung.deltakelseopplyser.statistikk.oppgave

import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveDAO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.stream.Collectors

@Service
class OppgaveStatistikkService(val oppgaveStatistikkRepository: OppgaveStatistikkRepository) {

    fun oppgaverMedSvarEllerEldreEnn14Dager(): List<OppgaveSvartidRecord> {
        val relevanteOppgaver =
            oppgaveStatistikkRepository.finnOppgaverMedSvarEllerEldreEnn14Dager()

        val records = ArrayList<OppgaveSvartidRecord>()

        finnLøsteOppgaverPrSvartid(relevanteOppgaver).forEach(records::add)
        finnLukketOppgavePrSvartid(relevanteOppgaver).forEach(records::add)
        finnOppgaverSomIkkeErLøstEllerLukketPåOver14Dager(relevanteOppgaver).forEach(records::add)

        return records
    }

    private fun finnOppgaverSomIkkeErLøstEllerLukketPåOver14Dager(relevanteOppgaver: List<OppgaveDAO>) =
        relevanteOppgaver.stream().filter { it.lukketDato == null && it.løstDato == null }
            .collect(Collectors.groupingBy { it.oppgavetype })
            .map {
                OppgaveSvartidRecord(
                    svartidAntallDager = null,
                    erLøst = false,
                    erLukket = false,
                    ikkeMottattOgEldreEnn14Dager = true,
                    oppgaveType = it.key,
                    antall = it.value.size,
                    opprettetTidspunkt = ZonedDateTime.now()
                )
            }

    private fun finnLukketOppgavePrSvartid(relevanteOppgaver: List<OppgaveDAO>) =
        relevanteOppgaver.stream().filter { it.lukketDato != null }
            .collect(Collectors.groupingBy {
                SvartidOgType(
                    it.opprettetDato.until(it.lukketDato!!, ChronoUnit.DAYS),
                    it.oppgavetype
                )
            })
            .entries
            .map {
                OppgaveSvartidRecord(
                    svartidAntallDager = it.key.svartid,
                    erLøst = false,
                    erLukket = true,
                    ikkeMottattOgEldreEnn14Dager = false,
                    oppgaveType = it.key.oppgavetype,
                    antall = it.value.size,
                    opprettetTidspunkt = ZonedDateTime.now()
                )
            }

    private fun finnLøsteOppgaverPrSvartid(relevanteOppgaver: List<OppgaveDAO>) =
        relevanteOppgaver.stream().filter { it.løstDato != null }
            .collect(Collectors.groupingBy {
                SvartidOgType(
                    it.opprettetDato.until(it.løstDato!!, ChronoUnit.DAYS),
                    it.oppgavetype
                )
            })
            .entries
            .map {
                OppgaveSvartidRecord(
                    svartidAntallDager = it.key.svartid,
                    erLøst = true,
                    erLukket = false,
                    ikkeMottattOgEldreEnn14Dager = false,
                    oppgaveType = it.key.oppgavetype,
                    antall = it.value.size,
                    opprettetTidspunkt = ZonedDateTime.now()
                )
            }

}


data class SvartidOgType(
    val svartid: Long,
    val oppgavetype: Oppgavetype
)