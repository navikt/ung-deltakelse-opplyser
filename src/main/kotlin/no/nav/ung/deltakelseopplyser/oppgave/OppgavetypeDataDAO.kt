package no.nav.ung.deltakelseopplyser.oppgave

import java.time.LocalDate

@OppgavetypeDataJsonType
sealed class OppgavetypeDataDAO

data class EndretStartdatoOppgavetypeDataDAO(
    val nyStartdato: LocalDate,
) : OppgavetypeDataDAO()

data class EndretSluttdatoOppgavetypeDataDAO(
    val nySluttdato: LocalDate,
) : OppgavetypeDataDAO()
