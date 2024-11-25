package no.nav.ung.deltakelseopplyser.register

import java.time.LocalDate
import java.util.*

data class DeltakelseInnmeldingDTO(
    val deltaker: DeltakerDTO,
    val startdato: LocalDate
) {

    override fun toString(): String {
        return "DeltakelseInnmeldingDTO(startdato=$startdato)"
    }
}
