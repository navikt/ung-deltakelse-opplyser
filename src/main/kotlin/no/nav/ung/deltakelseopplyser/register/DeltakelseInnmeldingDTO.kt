package no.nav.ung.deltakelseopplyser.register

import java.time.LocalDate

data class DeltakelseInnmeldingDTO(
    val deltakerIdent: String,
    val startdato: LocalDate
) {

    override fun toString(): String {
        return "DeltakelseInnmeldingDTO(startdato=$startdato)"
    }
}
