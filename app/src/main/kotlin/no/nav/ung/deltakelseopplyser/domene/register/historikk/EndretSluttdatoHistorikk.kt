package no.nav.ung.deltakelseopplyser.domene.register.historikk

import java.time.LocalDate

data class EndretSluttdatoHistorikk(
    val nySluttdato: LocalDate,
    val gammelSluttdato: LocalDate?
)
