package no.nav.ung.deltakelseopplyser.kontrakt.ekstern

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseStatus
import java.time.LocalDate
import java.util.*

data class DeltakelsePeriodeDTO(
    @JsonProperty("fraOgMed") val fraOgMed: LocalDate,
    @JsonProperty("tilOgMed") val tilOgMed: LocalDate? = null,
    @JsonProperty("harForlengetPeriode") val harForlengetPeriode: Boolean,
    @JsonProperty("periodeMaksDato") val periodeMaksDato: LocalDate,
    @JsonProperty("status") val status: DeltakelseStatus,
)

data class DeltakelseInfoDTO(
    @JsonProperty("deltakelseId") val deltakelseId: UUID,
    @JsonProperty("deltakerIdent") val deltakerIdent: String,
    @JsonProperty("periode") val periode: DeltakelsePeriodeDTO,
)

data class AlleDeltakelserResponseDTO(
    @JsonProperty("deltakelser") val deltakelser: List<DeltakelseInfoDTO>,
)

