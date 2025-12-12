package no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class PeriodeDTO (
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("startdato") val fom: LocalDate? = null,

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("startdato")val tom: LocalDate? = null
)