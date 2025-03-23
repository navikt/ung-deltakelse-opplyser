package no.nav.ung.deltakelseopplyser.domene.soknad.repository

import no.nav.ung.deltakelseopplyser.config.TxConfiguration.Companion.TRANSACTION_MANAGER
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional

@Transactional(TRANSACTION_MANAGER)
interface SøknadRepository : JpaRepository<UngSøknadDAO, String>
