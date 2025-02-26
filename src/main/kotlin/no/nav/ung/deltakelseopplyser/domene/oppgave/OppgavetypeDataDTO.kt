package no.nav.ung.deltakelseopplyser.domene.oppgave

import java.time.LocalDate

@OppgavetypeDataJsonType
interface OppgavetypeDataDTO

data class EndretStartdatoOppgavetypeDataDTO(
    val nyStartdato: LocalDate,
    val veilederRef: String,
    val meldingFraVeileder: String?,
) : OppgavetypeDataDTO

data class EndretSluttdatoOppgavetypeDataDTO(
    val nySluttdato: LocalDate,
    val veilederRef: String,
    val meldingFraVeileder: String?,
) : OppgavetypeDataDTO

fun OppgavetypeDataDAO.tilDTO(): OppgavetypeDataDTO = when (this) {
    is EndretStartdatoOppgavetypeDataDAO -> EndretStartdatoOppgavetypeDataDTO(nyStartdato, veilederRef, meldingFraVeileder)
    is EndretSluttdatoOppgavetypeDataDAO -> EndretSluttdatoOppgavetypeDataDTO(nySluttdato, veilederRef, meldingFraVeileder)
}
