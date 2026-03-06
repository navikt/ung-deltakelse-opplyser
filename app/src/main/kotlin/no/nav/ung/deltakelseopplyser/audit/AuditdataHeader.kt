package no.nav.ung.deltakelseopplyser.audit

/**
 * Loggversjon som endres ved nye felter. Ved bytte avtales nytt format med Arcsight-gjengen.
 */
class AuditdataHeader private constructor(
    private val vendor: String,
    private val product: String,
    private val eventClassId: EventClassId,
    private val name: String,
    private val severity: String,
) {
    /**
     * Loggheader i "Common Event Format (CEF)".
     */
    override fun toString(): String =
        "CEF:0|${cefHeaderEscape(vendor)}|${cefHeaderEscape(product)}|${cefHeaderEscape(LOG_VERSION)}|${cefHeaderEscape(eventClassId.cefKode)}|${cefHeaderEscape(name)}|${cefHeaderEscape(severity)}|"

    private companion object {
        private const val LOG_VERSION = "1.0"

        private fun cefHeaderEscape(s: String): String =
            s.replace("\\", "\\\\").replace("|", "\\|").replace("\n", "").replace("\r", "")
    }

    class Builder {
        private var vendor: String = ""
        private var product: String = ""
        private var eventClassId: EventClassId? = null
        private var name: String = ""
        private var severity: String = "INFO"

        fun medVendor(vendor: String) = apply { this.vendor = vendor }
        fun medProduct(product: String) = apply { this.product = product }
        fun medEventClassId(eventClassId: EventClassId) = apply { this.eventClassId = eventClassId }
        fun medName(name: String) = apply { this.name = name }
        fun medSeverity(severity: String) = apply { this.severity = severity }

        fun build(): AuditdataHeader = AuditdataHeader(
            vendor = requireNotNull(vendor.ifBlank { null }) { "vendor må være satt" },
            product = requireNotNull(product.ifBlank { null }) { "product må være satt" },
            eventClassId = requireNotNull(eventClassId) { "eventClassId må være satt" },
            name = requireNotNull(name.ifBlank { null }) { "name må være satt" },
            severity = severity,
        )
    }
}

