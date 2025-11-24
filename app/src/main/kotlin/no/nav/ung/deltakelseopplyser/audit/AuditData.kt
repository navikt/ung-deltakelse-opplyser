package no.nav.ung.deltakelseopplyser.audit

import java.util.stream.Collectors

/**
 * Data som utgjør et innslag i audtiloggen i "Common Event Format (CEF)".
 */
class AuditData(
    val header: AuditdataHeader,
    val fields: Set<CefField>
) {

    companion object {
        private val FIELD_SEPARATOR: String = " "
    }

    /**
     * Loggstreng i "Commen Event Format (CEF)".
     */
    override fun toString(): String {
        return header.toString() + fields.stream()
            .map { obj: CefField -> obj.toString() }
            .sorted()
            .collect(Collectors.joining(FIELD_SEPARATOR))
    }
}