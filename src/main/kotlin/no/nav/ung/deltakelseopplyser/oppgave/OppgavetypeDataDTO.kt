package no.nav.ung.deltakelseopplyser.oppgave

import java.time.LocalDate

@OppgavetypeDataJsonType
sealed class OppgavetypeDataDTO

data class EndretStartdatoOppgavetypeDataDTO(
    val nyStartdato: LocalDate,
) : OppgavetypeDataDTO()

data class EndretSluttdatoOppgavetypeDataDTO(
    val nySluttdato: LocalDate,
) : OppgavetypeDataDTO()

fun OppgavetypeDataDAO.tilDTO(): OppgavetypeDataDTO = when (this) {
    is EndretStartdatoOppgavetypeDataDAO -> EndretStartdatoOppgavetypeDataDTO(nyStartdato)
    is EndretSluttdatoOppgavetypeDataDAO -> EndretSluttdatoOppgavetypeDataDTO(nySluttdato)
}
