package no.nav.ung.deltakelseopplyser.audit

object CefFields {
    private const val BEHANDLING_TEXT = "Behandling"
    private const val SAKSNUMMER_TEXT = "Saksnummer"

    fun forSaksnummer(saksnummer: Long): Set<CefField> = forSaksnummer(saksnummer.toString())

    fun forSaksnummer(saksnummer: String): Set<CefField> = setOf(
        CefField(CefFieldName.SAKSNUMMER_VERDI, saksnummer),
        CefField(CefFieldName.SAKSNUMMER_LABEL, SAKSNUMMER_TEXT),
    )

    fun forBehandling(behandling: String): Set<CefField> = setOf(
        CefField(CefFieldName.BEHANDLING_VERDI, behandling),
        CefField(CefFieldName.BEHANDLING_LABEL, BEHANDLING_TEXT),
    )
}

