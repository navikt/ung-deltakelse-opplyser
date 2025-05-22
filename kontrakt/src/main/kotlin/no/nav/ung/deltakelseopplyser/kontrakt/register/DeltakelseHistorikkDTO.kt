package no.nav.ung.deltakelseopplyser.kontrakt.register

import java.time.LocalDate
import java.util.*

data class DeltakelseHistorikkDTO(
    val revisionType: HistorikkType,
    val revisionNumber: Optional<Long>,
    val id: UUID,
    val fom: LocalDate,
    val tom: LocalDate?,
    val opprettetAv: String?,
    val endretAv: String?,
)
