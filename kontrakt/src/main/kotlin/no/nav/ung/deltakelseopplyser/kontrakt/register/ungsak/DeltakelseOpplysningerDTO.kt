package no.nav.ung.deltakelseopplyser.kontrakt.register.ungsak

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseKomposittDTO

data class DeltakelseOpplysningerDTO(
    @JsonProperty("opplysninger") val opplysninger: List<DeltakelseKomposittDTO>,
)
