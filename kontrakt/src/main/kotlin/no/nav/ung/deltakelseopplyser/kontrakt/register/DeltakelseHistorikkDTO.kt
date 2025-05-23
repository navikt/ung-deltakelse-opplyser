package no.nav.ung.deltakelseopplyser.kontrakt.register

import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

data class DeltakelseHistorikkDTO(
    val revisjonstype: Revisjonstype,
    val revisjonsnummer: Long,
    val id: UUID,
    val fom: LocalDate,
    val tom: LocalDate?,
    val opprettetAv: String?,
    val opprettetTidspunkt: ZonedDateTime,
    val endretAv: String,
    val endretTidspunkt: ZonedDateTime,
    val søktTidspunkt: ZonedDateTime?,
)
