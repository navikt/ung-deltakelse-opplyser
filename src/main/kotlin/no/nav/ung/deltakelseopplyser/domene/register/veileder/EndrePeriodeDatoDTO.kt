package no.nav.ung.deltakelseopplyser.domene.register.veileder

import java.time.LocalDate

data class EndrePeriodeDatoDTO(
    val dato: LocalDate,
    val veilederRef: String,
    val meldingFraVeileder: String?,
)
