package no.nav.ung.deltakelseopplyser.domene.inntekt.repository

import no.nav.ung.deltakelseopplyser.config.TxConfiguration.Companion.TRANSACTION_MANAGER
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

@Transactional(TRANSACTION_MANAGER)
interface RapportertInntektRepository : JpaRepository<UngRapportertInntektDAO, String> {

    @Query(
        value = """
            SELECT u.*
            FROM ung_rapportert_inntekt u
            WHERE u.inntekt->>'søknadId' = :oppgaveReferanse
            LIMIT 1
        """,
        nativeQuery = true
    )
    fun finnRapportertInntektGittOppgaveReferanse(@Param("oppgaveReferanse") oppgaveReferanse: String): UngRapportertInntektDAO?
}
