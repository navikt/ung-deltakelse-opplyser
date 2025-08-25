package no.nav.ung.deltakelseopplyser.domene.register.historikk

import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseDAO
import no.nav.ung.deltakelseopplyser.kontrakt.register.historikk.DeltakelseHistorikkDTO
import no.nav.ung.deltakelseopplyser.kontrakt.register.historikk.Endringstype
import no.nav.ung.deltakelseopplyser.kontrakt.register.historikk.Revisjonstype
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class DeltakelseHistorikk(
    val deltakelse: DeltakelseDAO,
    val revisjonstype: Revisjonstype,
    val revisjonsnummer: Long,
    val opprettetAv: String,
    val opprettetTidspunkt: ZonedDateTime,
    val endretAv: String?,
    val endretTidspunkt: ZonedDateTime,
    val endringstype: Endringstype,
    val deltakerMeldtUt: DeltakerMeldtUtHistorikk?,
    val endretStartdato: EndretStartdatoHistorikk?,
    val endretSluttdato: EndretSluttdatoHistorikk?,
    val søktTidspunktSatt: SøktTidspunktHistorikk?,
) {

    companion object {
        val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.of("Europe/Oslo"))
    }

    fun tilDTO(): DeltakelseHistorikkDTO {
        return DeltakelseHistorikkDTO(
            tidspunkt = endretTidspunkt,
            endringstype = endringstype,
            revisjonstype = revisjonstype,
            endring = utledEndringsTekst(),
            aktør = endretAv ?: opprettetAv
        )
    }

    fun utledEndringsTekst(): String {
        return when (endringstype) {
            Endringstype.DELTAKER_MELDT_INN -> "Deltaker er meldt inn i programmet."

            Endringstype.DELTAKER_MELDT_UT -> {
                requireNotNull(deltakerMeldtUt)
                val utmeldingDato = deltakerMeldtUt.utmeldingDato
                "Deltaker meldt ut med sluttdato $utmeldingDato."
            }

            Endringstype.ENDRET_STARTDATO -> {
                requireNotNull(endretStartdato)
                "Startdato for deltakelse er endret fra ${endretStartdato.gammelStartdato} til ${endretStartdato.nyStartdato}."
            }

            Endringstype.ENDRET_SLUTTDATO -> {
                requireNotNull(endretSluttdato)
                return "Sluttdato for deltakelse er endret fra ${endretSluttdato.gammelSluttdato} til ${endretSluttdato.nySluttdato}."
            }

            Endringstype.DELTAKER_HAR_SØKT_YTELSE -> {
                requireNotNull(søktTidspunktSatt)
                val formatertTidspunkt = DATE_TIME_FORMATTER.format(søktTidspunktSatt.søktTidspunkt)
                "Deltaker har søkt om ytelse den ${formatertTidspunkt}."
            }

            Endringstype.UKJENT -> "Endringstype er ukjent."
        }
    }
}
