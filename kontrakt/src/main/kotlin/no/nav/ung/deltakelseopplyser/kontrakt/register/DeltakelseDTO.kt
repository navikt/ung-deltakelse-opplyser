package no.nav.ung.deltakelseopplyser.kontrakt.register

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

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

    @JsonProperty("harOpphørsvedtak")
    val harOpphørsvedtak: Boolean = false,

    @JsonProperty("harUtvidetKvote")
    val harUtvidetKvote: Boolean = false,

    @JsonProperty("søktTidspunkt")
    val søktTidspunkt: ZonedDateTime? = null,

    @JsonProperty("kvoteMaksDato")
    val kvoteMaksDato: LocalDate
) {
    override fun toString(): String =
        "DeltakelseDTO(id=$id, fraOgMed=$fraOgMed, tilOgMed=$tilOgMed)"
}

@Deprecated("Bruk DeltakelseDTO via v2-endepunkter. Oppgaver håndteres nå i ung-brukerdialog-api.")
data class DeltakelseKomposittDTO(
    @JsonUnwrapped
    val deltakelse: DeltakelseDTO,

    @JsonProperty("oppgaver")
    val oppgaver: List<Any> = emptyList()
) {
    override fun toString(): String =
        "DeltakelseKomposittDTO(id=${deltakelse.id}, fraOgMed=${deltakelse.fraOgMed}, tilOgMed=${deltakelse.tilOgMed})"
}
