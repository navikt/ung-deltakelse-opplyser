package no.nav.ung.deltakelseopplyser.statistikk.deltaker

import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDAO
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface DeltakerStatistikkRepository : JpaRepository<DeltakerDAO, UUID> {


    /**
     * Henter aggregeringen av oppgaveantall og antall deltakere.
     * Grupperer deltakere etter antall oppgaver.
     *
     * @return En liste av AntallOppgaverAntallDeltakereFordeling som inneholder aggregeringen.
     */
    @Query(
        """
    SELECT 
        t.oppgave_count AS antallOppgaver,
        COUNT(*)        AS antallDeltakere
    FROM (
        SELECT d.id, COUNT(o.id) AS oppgave_count
        FROM deltaker d
        LEFT JOIN oppgave o ON o.deltaker_id = d.id
        GROUP BY d.id
    ) t
    GROUP BY t.oppgave_count
    ORDER BY t.oppgave_count DESC
    """,
        nativeQuery = true
    )
    fun antallDeltakereEtterAntallOppgaverFordeling(): List<AntallOppgaverAntallDeltakereFordeling>


    /**
     * Henter antall deltakere per oppgavetype.
     *
     * @return En liste over antall deltakere per oppgavetype.
     */
    @Query(
        """
        SELECT 
          o.oppgavetype           AS oppgavetype,
          COUNT(DISTINCT o.deltaker_id) AS antallDeltakere
        FROM oppgave o
        GROUP BY o.oppgavetype
        ORDER BY antallDeltakere DESC, oppgavetype
        """,
        nativeQuery = true
    )
    fun antallDeltakerePerOppgavetype(): List<AntallDeltakerePerOppgavetype>
}
