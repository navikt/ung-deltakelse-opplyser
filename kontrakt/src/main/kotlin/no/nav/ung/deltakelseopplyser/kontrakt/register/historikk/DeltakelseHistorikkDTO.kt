package no.nav.ung.deltakelseopplyser.kontrakt.register.historikk

import java.time.ZonedDateTime

data class DeltakelseHistorikkDTO(
    val tidspunkt: ZonedDateTime,
    val endringstype: Endringstype,
    val revisjonstype: Revisjonstype,
    val endring: String,
    val aktør: String
)
