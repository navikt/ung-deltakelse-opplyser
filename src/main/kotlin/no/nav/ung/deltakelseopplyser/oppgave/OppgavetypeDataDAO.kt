package no.nav.ung.deltakelseopplyser.oppgave

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

@OppgavetypeDataJsonType
sealed class OppgavetypeDataDAO

data class EndretStartdatoOppgavetypeDataDAO(
    val nyStartdato: LocalDate,
    @JsonProperty(defaultValue = "n/a") val veilederRef: String = "n/a",
    val meldingFraVeileder: String?,
) : OppgavetypeDataDAO()

data class EndretSluttdatoOppgavetypeDataDAO(
    val nySluttdato: LocalDate,
    @JsonProperty(defaultValue = "n/a") val veilederRef: String,
    val meldingFraVeileder: String?,
) : OppgavetypeDataDAO()
