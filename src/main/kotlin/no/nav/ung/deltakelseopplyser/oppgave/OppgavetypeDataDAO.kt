package no.nav.ung.deltakelseopplyser.oppgave

import java.time.LocalDate

@OppgavetypeDataJsonType
sealed class OppgavetypeDataDAO

data class EndretStartdatoOppgavetypeDataDAO(
    val nyStartdato: LocalDate,
    val veilederRef: String,
    val meldingFraVeileder: String?,
) : OppgavetypeDataDAO()

data class EndretSluttdatoOppgavetypeDataDAO(
    val nySluttdato: LocalDate,
    val veilederRef: String,
    val meldingFraVeileder: String?,
) : OppgavetypeDataDAO()
