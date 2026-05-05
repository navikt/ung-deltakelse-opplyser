package no.nav.ung.deltakelseopplyser.kontrakt.register.ungsak

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

/**
 * DTO for å sette sluttdato på en deltakelse fra ung-sak.
 * Brukes ved automatisk opphør av ungdomsprogramytelsen.
 */
data class SettSluttdatoDTO(
    @JsonProperty("sluttdato") val sluttdato: LocalDate
)
