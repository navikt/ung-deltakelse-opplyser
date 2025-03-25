package no.nav.ung.deltakelseopplyser.domene.register

import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDTO
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDTO.Companion.mapToDTO
import no.nav.ung.deltakelseopplyser.domene.oppgave.OppgaveDTO
import no.nav.ung.deltakelseopplyser.domene.oppgave.OppgaveDTO.Companion.tilDTO
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
    companion object {
        fun UngdomsprogramDeltakelseDAO.mapToDTO(): DeltakelseOpplysningDTO {

            return DeltakelseOpplysningDTO(
                id = id,
                deltaker = deltaker.mapToDTO(),
                harSøkt = harSøkt,
                fraOgMed = getFom(),
                tilOgMed = getTom(),
                oppgaver = oppgaver.map { it.tilDTO() }
            )
        }
    }

    override fun toString(): String {
        return "DeltakerProgramOpplysningDTO(id=$id, fraOgMed=$fraOgMed, tilOgMed=$tilOgMed)"
    }
}
