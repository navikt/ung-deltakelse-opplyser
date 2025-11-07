package no.nav.ung.deltakelseopplyser.audit


class AuditdataHeader(
    val vendor: String,
    val product: String,
    val eventClassId: EventClassId,
    val name: String,
    val severity: String
) {

    companion object {

        /**
         * Loggversjon som endres ved nye felter. Ved bytte avtales nytt format med
         * Arcsight-gjengen.
         */
        const val LOG_VERISON: String = "1.0"
    }

    /**
     * Loggheader i "Commen Event Format (CEF)".
     */
    override fun toString(): String {
        return String.format(
            "CEF:0|%s|%s|%s|%s|%s|%s|",
            cefHeaderEscape(vendor),
            cefHeaderEscape(product),
            cefHeaderEscape(LOG_VERISON),
            cefHeaderEscape(eventClassId.cefKode),
            cefHeaderEscape(name),
            cefHeaderEscape(severity)
        )
    }

    private fun cefHeaderEscape(s: String): String {
        return s.replace("\\", "\\\\").replace("|", "\\|").replace("\n", "").replace("\r", "")
    }


    class Builder() {
        private var vendor: String? = null
        private var product: String? = null
        private var eventClassId: EventClassId? = null
        private var name: String? = null
        private var severity = "INFO"

        fun medVendor(vendor: String): Builder {
            this.vendor = vendor
            return this
        }

        fun medProduct(product: String): Builder {
            this.product = product
            return this
        }

        fun medEventClassId(eventClassId: EventClassId): Builder {
            this.eventClassId = eventClassId
            return this
        }

        fun medName(name: String): Builder {
            this.name = name
            return this
        }

        fun medSeverity(severity: String): Builder {
            this.severity = severity
            return this
        }

        fun build(): AuditdataHeader {
            return AuditdataHeader(vendor!!, product!!, eventClassId!!, name!!, severity)
        }
    }

}