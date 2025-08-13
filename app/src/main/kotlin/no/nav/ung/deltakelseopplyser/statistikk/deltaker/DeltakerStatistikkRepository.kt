package no.nav.ung.deltakelseopplyser.statistikk.deltaker

import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDAO
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface DeltakerStatistikkRepository : JpaRepository<DeltakerDAO, UUID> {

    /**
     * Alle deltakere i ungdomsprogrammet.
     * Dagens dato skal være innafor deltakelsesperioden.
     */
    @Query(
        """
        SELECT COUNT(DISTINCT d.deltaker_ident) 
        FROM deltaker d 
        INNER JOIN ungdomsprogram_deltakelse deltakelse on d.id = deltakelse.deltaker_id
        WHERE deltakelse.periode @> CURRENT_DATE
          """,
        nativeQuery = true
    )
    fun antallDeltakereIUngdomsprogrammet(): Long


    /**
     * Henter antall deltakere per oppgavetype.
     *
     * @return En liste over antall deltakere per oppgavetype.
     */
    @Query(
        """
        SELECT 
          o.oppgavetype           AS oppgavetype,
          o.status                AS status,
          COUNT(DISTINCT o.deltaker_id) AS antallDeltakere
        FROM oppgave o
        GROUP BY o.oppgavetype, o.status
        ORDER BY antallDeltakere DESC, oppgavetype, status
        """,
        nativeQuery = true
    )
    fun antallDeltakerePerOppgavetype(): List<AntallDeltakerePerOppgavetype>
}
