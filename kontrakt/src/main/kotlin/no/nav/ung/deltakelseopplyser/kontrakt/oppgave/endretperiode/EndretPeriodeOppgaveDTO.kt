package no.nav.ung.deltakelseopplyser.kontrakt.oppgave.endretperiode

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.PeriodeDTO
import java.time.LocalDateTime
import java.util.*

data class EndretPeriodeOppgaveDTO(
    @JsonProperty("deltakerIdent") val deltakerIdent: String,
    @JsonProperty("oppgaveReferanse") val oppgaveReferanse: UUID,
    @JsonProperty("frist") val frist: LocalDateTime,
    @JsonProperty("nyPeriode") val nyPeriode: PeriodeDTO?,
    @JsonProperty("forrigePeriode") val forrigePeriode: PeriodeDTO? = null,
)