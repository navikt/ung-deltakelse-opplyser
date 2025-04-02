package no.nav.ung.deltakelseopplyser.kontrakt.register

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveDTO
import java.time.LocalDate
import java.util.*

data class DeltakerOpplysningerDTO(
    @JsonProperty("opplysninger") val opplysninger: List<DeltakelseOpplysningDTO>,
)

data class DeltakelseOpplysningDTO(
    @JsonProperty("id") val id: UUID? = null,
    @JsonProperty("deltaker") val deltaker: DeltakerDTO,
    @JsonProperty("fraOgMed") val fraOgMed: LocalDate,
    @JsonProperty("tilOgMed") val tilOgMed: LocalDate? = null,
    @JsonProperty("harSøkt") val harSøkt: Boolean,
    @JsonProperty("oppgaver") val oppgaver: List<OppgaveDTO>,
) {

    override fun toString(): String {
        return "DeltakerProgramOpplysningDTO(id=$id, fraOgMed=$fraOgMed, tilOgMed=$tilOgMed)"
    }
}
