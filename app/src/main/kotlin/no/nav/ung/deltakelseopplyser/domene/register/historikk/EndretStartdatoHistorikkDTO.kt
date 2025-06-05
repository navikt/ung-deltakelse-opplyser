package no.nav.ung.deltakelseopplyser.domene.register.historikk

import java.time.LocalDate

data class EndretStartdatoHistorikkDTO(
    val nyStartdato: LocalDate,
    val gammelStartdato: LocalDate
)
