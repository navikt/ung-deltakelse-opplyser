package no.nav.ung.deltakelseopplyser.kontrakt.veileder

import java.time.LocalDate

data class EndrePeriodeDatoDTO(
    val dato: LocalDate,
    val veilederRef: String,
    val meldingFraVeileder: String?,
)
