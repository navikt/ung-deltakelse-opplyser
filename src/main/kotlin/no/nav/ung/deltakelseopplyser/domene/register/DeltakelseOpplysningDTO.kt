package no.nav.ung.deltakelseopplyser.domene.register

import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDTO
import no.nav.ung.deltakelseopplyser.domene.oppgave.OppgaveDTO
import java.time.LocalDate
import java.util.*

data class DeltakerOpplysningerDTO(
    val opplysninger: List<DeltakelseOpplysningDTO>,
)

data class DeltakelseOpplysningDTO(
    val id: UUID? = null,
    val deltaker: DeltakerDTO,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate? = null,
    val harSøkt: Boolean,
    val oppgaver: List<OppgaveDTO>,
) {

    override fun toString(): String {
        return "DeltakerProgramOpplysningDTO(id=$id, fraOgMed=$fraOgMed, tilOgMed=$tilOgMed)"
    }
}
