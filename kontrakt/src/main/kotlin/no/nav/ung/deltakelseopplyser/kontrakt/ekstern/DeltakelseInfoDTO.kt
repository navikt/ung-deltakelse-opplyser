package no.nav.ung.deltakelseopplyser.kontrakt.ekstern

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseStatus
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

data class DeltakelsePeriodeDTO(
    @JsonProperty("fraOgMed") val fraOgMed: LocalDate,
    @JsonProperty("tilOgMed") val tilOgMed: LocalDate? = null,
    @JsonProperty("harForlengetPeriode") val harForlengetPeriode: Boolean,
    @JsonProperty("periodeMaksDato") val periodeMaksDato: LocalDate,
    private val søktTidspunkt: ZonedDateTime? = null,
) {

    @get:JsonProperty("status")
    val status: DeltakelseStatus get() = DeltakelseStatus.utledFra(søktTidspunkt, tilOgMed, periodeMaksDato)
}

data class DeltakelseInfoDTO(
    @JsonProperty("deltakelseId") val deltakelseId: UUID,
    @JsonProperty("deltakerIdent") val deltakerIdent: String,
    @JsonProperty("periode") val periode: DeltakelsePeriodeDTO,
)

data class AlleDeltakelserResponseDTO(
    @JsonProperty("deltakelser") val deltakelser: List<DeltakelseInfoDTO>,
)

