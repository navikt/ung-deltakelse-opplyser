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

fun OppgavetypeData.tilDTO(): OppgavetypeDataDTO = when (this) {
    is EndretStartdatoOppgavetypeData -> EndretStartdatoOppgavetypeDataDTO(nyStartdato)
    is EndretSluttdatoOppgavetypeData -> EndretSluttdatoOppgavetypeDataDTO(nySluttdato)
}
