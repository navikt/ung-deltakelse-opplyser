package no.nav.ung.deltakelseopplyser.audit

/**
 * Nøkkel og verdi i "Common Event Format (CEF)".
 */
class CefField(
    val key: CefFieldName,
    val value: String?,
) {
    constructor(key: CefFieldName, value: Long) : this(key, value.toString())

    /**
     * Nøkkel og verdi i "Common Event Format (CEF)".
     */
    override fun toString(): String {
        if (value == null) return ""
        return "${key.kode}=${cefValueEscape(value)}"
    }

    private companion object {
        private fun cefValueEscape(s: String): String =
            s.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r").replace("=", "\\=")
    }
}

