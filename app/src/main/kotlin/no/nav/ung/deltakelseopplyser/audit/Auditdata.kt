package no.nav.ung.deltakelseopplyser.audit

/**
 * Data som utgjør et innslag i auditloggen i "Common Event Format (CEF)".
 */
class Auditdata(
    private val header: AuditdataHeader,
    fields: Set<CefField>,
) {
    private val fields: Set<CefField> = HashSet(fields)

    /**
     * Loggstreng i "Common Event Format (CEF)".
     */
    override fun toString(): String =
        header.toString() + fields
            .map { it.toString() }
            .sorted()
            .joinToString(FIELD_SEPARATOR)

    private companion object {
        private const val FIELD_SEPARATOR = " "
    }
}

