package no.nav.ung.deltakelseopplyser.audit

import no.nav.k9.felles.log.audit.Auditdata
import no.nav.k9.felles.log.audit.AuditdataHeader
import no.nav.k9.felles.log.audit.CefField
import no.nav.k9.felles.log.audit.CefFieldName
import no.nav.k9.felles.log.audit.EventClassId
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.sif.abac.kontrakt.person.PersonIdent
import no.nav.ung.deltakelseopplyser.utils.navIdent
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class SporingsloggService(
    private val auditlogger: Auditlogger,
    private val tokenValidationContextHolder: SpringTokenValidationContextHolder,
) {

    fun loggLesetilgang(url: String, beskrivelse: String, bruker: PersonIdent) {
        auditlogger.logg(
            Auditdata(
                AuditdataHeader.Builder()
                    .medVendor(auditlogger.vendor)
                    .medProduct(auditlogger.product)
                    .medSeverity("INFO")
                    .medName(beskrivelse)
                    .medEventClassId(EventClassId.AUDIT_ACCESS)
                    .build(),
                setOf(
                    CefField(CefFieldName.EVENT_TIME, LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) * 1000L),
                    CefField(CefFieldName.REQUEST, url),
                    CefField(CefFieldName.USER_ID, tokenValidationContextHolder.navIdent()),
                    CefField(CefFieldName.BERORT_BRUKER_ID, bruker.ident)
                )
            )
        )
    }

}
