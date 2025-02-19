package no.nav.ung.deltakelseopplyser.register

import no.nav.ung.deltakelseopplyser.oppgave.OppgaveDTO
import java.time.LocalDate
import java.util.*

data class DeltakerOpplysningerDTO(
    val opplysninger: List<DeltakelseOpplysningDTO>,
)

data class DeltakelseOpplysningDTO(
    val id: UUID? = null,
    val deltaker: DeltakerDTO,
    val harSøkt: Boolean,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate? = null,
    val oppgaver: List<OppgaveDTO>,
) {

    override fun toString(): String {
        return "DeltakerProgramOpplysningDTO(id=$id, fraOgMed=$fraOgMed, tilOgMed=$tilOgMed)"
    }
}
