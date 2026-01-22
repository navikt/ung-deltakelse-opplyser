package no.nav.ung.deltakelseopplyser.domene.register.historikk

import java.time.LocalDate

data class DeltakelseFjernetHistorikk(
    val forrigeStartdato: LocalDate,
    val forrigeSluttdato: LocalDate?
)
