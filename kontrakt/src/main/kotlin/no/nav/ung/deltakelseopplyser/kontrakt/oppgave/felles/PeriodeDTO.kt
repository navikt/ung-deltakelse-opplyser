package no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class PeriodeDTO (
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("fom") val fom: LocalDate,

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("tom")val tom: LocalDate? = null
)