package no.nav.ung.deltakelseopplyser.kontrakt.veileder

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class DeltakelseInnmeldingDTO(
    @JsonProperty("deltakerIdent") val deltakerIdent: String,
    @JsonProperty("startdato") val startdato: LocalDate
) {

    override fun toString(): String {
        return "DeltakelseInnmeldingDTO(startdato=$startdato)"
    }
}
