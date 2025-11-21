package no.nav.ung.deltakelseopplyser.kontrakt.register

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveDTO
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

/**
 * Kjernestruktur uten oppgaver
 */
data class DeltakelseDTO(
    @JsonProperty("id")
    val id: UUID? = null,

    @JsonProperty("deltaker")
    val deltaker: DeltakerDTO,

    @JsonProperty("fraOgMed")
    val fraOgMed: LocalDate,

    @JsonProperty("tilOgMed")
    val tilOgMed: LocalDate? = null,

    @JsonProperty("erSlettet")
    val erSlettet: Boolean = false,

    @JsonProperty("søktTidspunkt")
    val søktTidspunkt: ZonedDateTime? = null
) {
    override fun toString(): String =
        "DeltakelseDTO(id=$id, fraOgMed=$fraOgMed, tilOgMed=$tilOgMed)"
}

/**
 * Komposittstruktur som også inneholder oppgaver
 */
data class DeltakelseKomposittDTO(
    @JsonUnwrapped
    val deltakelse: DeltakelseDTO,

    @JsonProperty("oppgaver")
    val oppgaver: List<OppgaveDTO>
) {
    override fun toString(): String =
        "DeltakelseKomposittDTO(id=${deltakelse.id}, fraOgMed=${deltakelse.fraOgMed}, tilOgMed=${deltakelse.tilOgMed}, antallOppgaver=${oppgaver.size})"
}
