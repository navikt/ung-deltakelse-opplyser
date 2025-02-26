package no.nav.ung.deltakelseopplyser.domene.register

import java.time.LocalDate
import java.util.*

data class DeltakelseUtmeldingDTO(
    val utmeldingsdato: LocalDate,
) {

    override fun toString(): String {
        return "DeltakelseUtmeldingDTO(utmeldingsdato=$utmeldingsdato)"
    }
}
