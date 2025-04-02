package no.nav.ung.deltakelseopplyser.kontrakt.deltaker

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class DeltakerDTO(
    @JsonProperty("id") val id: UUID ? = null,
    @JsonProperty("deltakerIdent") val deltakerIdent: String,
)
