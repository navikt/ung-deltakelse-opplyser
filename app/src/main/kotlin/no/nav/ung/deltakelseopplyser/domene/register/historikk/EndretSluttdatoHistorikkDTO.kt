package no.nav.ung.deltakelseopplyser.domene.register.historikk

import java.time.LocalDate

data class EndretSluttdatoHistorikkDTO(
    val nySluttdato: LocalDate,
    val gammelSluttdato: LocalDate?
)
