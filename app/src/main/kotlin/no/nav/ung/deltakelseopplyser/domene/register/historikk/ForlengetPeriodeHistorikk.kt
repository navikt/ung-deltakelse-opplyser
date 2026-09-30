package no.nav.ung.deltakelseopplyser.domene.register.historikk

import java.time.LocalDate

data class ForlengetPeriodeHistorikk(
    val forlengetFraOgMed: LocalDate,
    val forlengetTilOgMed: LocalDate,
)

