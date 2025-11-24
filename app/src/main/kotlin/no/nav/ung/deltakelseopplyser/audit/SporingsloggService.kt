package no.nav.ung.deltakelseopplyser.audit

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

    fun logg(url: String, beskrivelse: String, bruker: PersonIdent, eventClassId: EventClassId) {
        auditlogger.logg(
            AuditData(
                AuditdataHeader.Builder()
                    .medVendor(auditlogger.vendor)
                    .medProduct(auditlogger.product)
                    .medSeverity("INFO")
                    .medName(beskrivelse)
                    .medEventClassId(eventClassId)
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
