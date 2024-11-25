package no.nav.ung.deltakelseopplyser.register

import java.time.LocalDate
import java.util.*

data class DeltakelseInnmeldingDTO(
    val deltakerIdent: String,
    val startdato: LocalDate
) {

    override fun toString(): String {
        return "DeltakelseInnmeldingDTO(deltakerIdentSatt='${deltakerIdent.isNotBlank()}', startdato=$startdato)"
    }
}
