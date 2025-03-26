package no.nav.ung.deltakelseopplyser.domene.inntekt.repository

import no.nav.ung.deltakelseopplyser.config.TxConfiguration.Companion.TRANSACTION_MANAGER
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional

@Transactional(TRANSACTION_MANAGER)
interface RapportertInntektRepository : JpaRepository<UngRapportertInntektDAO, String>
