package no.nav.ung.deltakelseopplyser.oppgave

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

@OppgavetypeDataJsonType
sealed class OppgavetypeDataDAO

data class EndretStartdatoOppgavetypeDataDAO(
    @JsonFormat(pattern = "yyyy-MM-dd") val nyStartdato: LocalDate,
    @JsonProperty(defaultValue = "n/a") val veilederRef: String = "n/a",
    val meldingFraVeileder: String?,
) : OppgavetypeDataDAO()

data class EndretSluttdatoOppgavetypeDataDAO(
    @JsonFormat(pattern = "yyyy-MM-dd")val nySluttdato: LocalDate,
    @JsonProperty(defaultValue = "n/a") val veilederRef: String,
    val meldingFraVeileder: String?,
) : OppgavetypeDataDAO()
