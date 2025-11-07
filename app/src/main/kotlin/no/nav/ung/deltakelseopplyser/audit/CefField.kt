package no.nav.ung.deltakelseopplyser.audit


class CefField (
    private val key: CefFieldName,
    private val value: String) {

    constructor(key: CefFieldName, value: Long) : this(key, value.toString())

    private fun cefValueEscape(s: String): String {
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r").replace("=", "\\=")
    }

    /**
     * Nøkkel og verdi i "Commen Event Format (CEF)".
     */
    override fun toString(): String {
        return key.cefKode + "=" + cefValueEscape(value)
    }
}