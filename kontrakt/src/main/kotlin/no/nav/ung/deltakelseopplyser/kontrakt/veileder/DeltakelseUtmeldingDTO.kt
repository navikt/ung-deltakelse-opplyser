package no.nav.ung.deltakelseopplyser.kontrakt.veileder

import java.time.LocalDate

data class DeltakelseUtmeldingDTO(
    val utmeldingsdato: LocalDate,
) {

    override fun toString(): String {
        return "DeltakelseUtmeldingDTO(utmeldingsdato=$utmeldingsdato)"
    }
}
