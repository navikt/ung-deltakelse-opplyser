package no.nav.ung.deltakelseopplyser.statistikk.deltaker

import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDAO
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface DeltakerStatistikkRepository : JpaRepository<DeltakerDAO, UUID> {

    /**
     * Alle deltakere i ungdomsprogrammet.
     * Inkluderer deltakere som har en periode som slutter i fremtiden eller som ikke har en sluttidspunkt
     */
    @Query(
        """
        SELECT COUNT(DISTINCT d.deltaker_ident) 
        FROM deltaker d 
        INNER JOIN ungdomsprogram_deltakelse deltakelse on d.id = deltakelse.deltaker_id
        WHERE upper_inf(deltakelse.periode) OR upper(deltakelse.periode) > CURRENT_DATE
          """,
        nativeQuery = true
    )
    fun antallDeltakereIUngdomsprogrammet(): Long
}
