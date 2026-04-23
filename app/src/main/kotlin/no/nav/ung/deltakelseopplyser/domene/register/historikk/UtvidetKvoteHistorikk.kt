package no.nav.ung.deltakelseopplyser.domene.register.historikk

import java.time.LocalDate

data class UtvidetKvoteHistorikk(
    val utvidetFraOgMed: LocalDate,
    val utvidetTilOgMed: LocalDate,
)

