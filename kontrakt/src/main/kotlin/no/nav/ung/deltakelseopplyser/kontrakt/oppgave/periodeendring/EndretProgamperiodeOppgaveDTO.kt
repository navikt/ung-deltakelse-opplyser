package no.nav.ung.deltakelseopplyser.kontrakt.oppgave.periodeendring

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class EndretProgamperiodeOppgaveDTO(
    @JsonProperty("deltakerIdent") val deltakerIdent: String,
    @JsonProperty("oppgaveReferanse") val oppgaveReferanse: UUID,
    @JsonProperty("frist") val frist: LocalDateTime,
    @JsonProperty("programperiode") val programperiode: ProgramperiodeDTO,
)

data class ProgramperiodeDTO (
    @JsonProperty("fomDato") val fomDato: LocalDate,
    @JsonProperty("tomDato") val tomDato: LocalDate? = null,
)