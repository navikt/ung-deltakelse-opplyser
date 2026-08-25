package no.nav.ung.deltakelseopplyser.kontrakt.register

import com.fasterxml.jackson.annotation.JsonAlias
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

    @JsonProperty("harForlengetPeriode")
    @JsonAlias("harUtvidetKvote")
    val harForlengetPeriode: Boolean = false,

    @JsonProperty("søktTidspunkt")
    val søktTidspunkt: ZonedDateTime? = null,

    @JsonProperty("periodeMaksDato")
    @JsonAlias("forlengetPeriodeMaksDato", "kvoteMaksDato")
    val periodeMaksDato: LocalDate,

    @JsonProperty("avslutningsårsak")
    val avslutningsårsak: Avslutningsårsak? = null,
) {

    /** @deprecated Bruk [periodeMaksDato]. Beholdt for bakoverkompatibilitet. */
    @Deprecated("Bruk periodeMaksDato", ReplaceWith("periodeMaksDato"))
    @get:JsonProperty("forlengetPeriodeMaksDato")
    val forlengetPeriodeMaksDato: LocalDate get() = periodeMaksDato

    /** @deprecated Bruk [harForlengetPeriode]. Beholdt for bakoverkompatibilitet. */
    @Deprecated("Bruk harForlengetPeriode", ReplaceWith("harForlengetPeriode"))
    @get:JsonProperty("harUtvidetKvote")
    val harUtvidetKvote: Boolean get() = harForlengetPeriode

    /** @deprecated Bruk [periodeMaksDato]. Beholdt for bakoverkompatibilitet. */
    @Deprecated("Bruk periodeMaksDato", ReplaceWith("periodeMaksDato"))
    @get:JsonProperty("kvoteMaksDato")
    val kvoteMaksDato: LocalDate get() = periodeMaksDato

    /**
     * Status for deltakelsen, utledet av [tilOgMed] og [periodeMaksDato].
     *
     * - [DeltakelseStatus.LØPENDE]: [tilOgMed] er ikke satt.
     * - [DeltakelseStatus.VIL_AVSLUTTES]: sluttdato ([tilOgMed] eller, hvis den ikke er satt,
     *   [periodeMaksDato]) er satt og ligger frem i tid.
     * - [DeltakelseStatus.AVSLUTTET]: sluttdatoen har passert (dagen etter sluttdato regnes som avsluttet).
     */
    @get:JsonProperty("status")
    val status: DeltakelseStatus
        get() {
            val effektivSluttdato = tilOgMed ?: periodeMaksDato
            return when {
                effektivSluttdato.isBefore(LocalDate.now()) -> DeltakelseStatus.AVSLUTTET
                tilOgMed != null -> DeltakelseStatus.VIL_AVSLUTTES
                else -> DeltakelseStatus.LØPENDE
            }
        }

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
    /**
     * Eksponerer [DeltakelseDTO.status] direkte på komposittobjektet for enklere programmatisk
     * tilgang. Serialiseres allerede via [JsonUnwrapped] på [deltakelse], så denne har ingen
     * JSON-annotasjon for å unngå duplikat felt.
     */
    val status: DeltakelseStatus get() = deltakelse.status

    override fun toString(): String =
        "DeltakelseKomposittDTO(id=${deltakelse.id}, fraOgMed=${deltakelse.fraOgMed}, tilOgMed=${deltakelse.tilOgMed}, status=$status)"
}
