package no.nav.ung.deltakelseopplyser.statistikk.oppgave

import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveDAO
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.ZonedDateTime
import java.util.*

interface OppgaveStatistikkRepository : JpaRepository<OppgaveDAO, UUID> {

    @Query(
        value = """
        SELECT o.* from oppgave o
        WHERE o.løst_dato is not null 
or o.lukket_dato is not null 
or o.opprettet_dato < (CURRENT_DATE - INTERVAL '14 days') and o.status in ('LØST', 'ULØST', 'LUKKET', 'UTLØPT')
    """,
        nativeQuery = true
    )
    fun finnOppgaverMedSvarEllerEldreEnn14Dager(): List<OppgaveDAO>


    @Query(
        value =
            """
                SELECT o.* from oppgave o
                WHERE o.oppgavetype = :oppgaveType
                and (
                    o.løst_dato > :sisteKjoringTidspunkt 
                    or o.lukket_dato > :sisteKjoringTidspunkt
                    or o.opprettet_dato > :sisteKjoringTidspunkt
                )
            """,
        nativeQuery = true
    )
    fun finnOppgaverMedEndringSidenSisteKjøring(
        sisteKjoringTidspunkt: ZonedDateTime,
        oppgaveType: String
    ): List<OppgaveDAO>
}