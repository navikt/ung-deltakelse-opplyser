package no.nav.ung.deltakelseopplyser.domene.register.historikk

import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseDAO
import no.nav.ung.deltakelseopplyser.kontrakt.register.historikk.Endringstype
import java.util.*

object DeltakelseHistorikkEndringUtleder {

    fun utledEndring(
        nåværendeDeltakelseRevisjon: DeltakelseDAO,
        forrigeDeltakelseRevisjon: DeltakelseDAO?,
    ): HistorikkEndring {
        // Sammenligner feltene for å finne ut hva som har endret seg
        val startdatoErEndret = if (forrigeDeltakelseRevisjon != null) {
            val forrigeStartdato = forrigeDeltakelseRevisjon.getFom()
            val nåværendeStartdato = nåværendeDeltakelseRevisjon.getFom()

            forrigeStartdato != nåværendeStartdato
        } else {
            // Hvis det ikke finnes en forrige revisjon, betyr det at dette er den første revisjonen
            // og at startdatoen er satt for første gang.
            false
        }

        val forrigeSluttdato = forrigeDeltakelseRevisjon?.getTom()
        val nåværendeSluttdato = nåværendeDeltakelseRevisjon.getTom()
        val deltakerMeldtUt = forrigeSluttdato == null && nåværendeSluttdato != null
        val sluttdatoErEndret = forrigeSluttdato != null && forrigeSluttdato != nåværendeSluttdato

        val soktTidspunktErEndret =
            forrigeDeltakelseRevisjon?.søktTidspunkt != nåværendeDeltakelseRevisjon.søktTidspunkt

        // Lag liste med navn på de feltene som faktisk endret seg
        val endredeFelter = listOfNotNull(
            "startdato".takeIf { startdatoErEndret },
            "sluttdatoSatt".takeIf { deltakerMeldtUt },
            "sluttdatoEndret".takeIf { sluttdatoErEndret },
            "søktTidspunkt".takeIf { soktTidspunktErEndret }
        )

        håndterFlereEndringerISammeRevisjon(endredeFelter, nåværendeDeltakelseRevisjon.id)

        return HistorikkEndring(
            endringstype = utledEndringstype(
                forrigeDeltakelseRevisjon,
                startdatoErEndret,
                deltakerMeldtUt,
                sluttdatoErEndret,
                soktTidspunktErEndret
            ),

            endretStartdatoData = utledEndretStartdatoHistorikkDTO(
                startdatoErEndret,
                forrigeDeltakelseRevisjon,
                nåværendeDeltakelseRevisjon
            ),

            deltakerMeldtUtData = utledDeltakerMeldtUtHistorikk(deltakerMeldtUt, nåværendeDeltakelseRevisjon),

            endretSluttdatoData = utledEndretSluttdatoHistorikkDTO(
                sluttdatoErEndret,
                forrigeDeltakelseRevisjon,
                nåværendeDeltakelseRevisjon
            ),

            søktTidspunktSatt = utledSøktTidspunktHistorikkDTO(soktTidspunktErEndret, nåværendeDeltakelseRevisjon)
        )
    }

    private fun utledDeltakerMeldtUtHistorikk(deltakerMeldtUt: Boolean, nåværendeDeltakelseRevisjon: DeltakelseDAO): DeltakerMeldtUtHistorikk? {
        return if (deltakerMeldtUt) {
            DeltakerMeldtUtHistorikk(
                utmeldingDato = nåværendeDeltakelseRevisjon.getTom()!! // Sluttdato kan ikke være null ved utmelding
            )
        } else null
    }

    private fun utledSøktTidspunktHistorikkDTO(
        soktTidspunktErEndret: Boolean,
        nåværendeDeltakelseRevisjon: DeltakelseDAO,
    ) = if (soktTidspunktErEndret) {
        SøktTidspunktHistorikk(
            søktTidspunktSatt = soktTidspunktErEndret,
            søktTidspunkt = nåværendeDeltakelseRevisjon.søktTidspunkt!!
        )
    } else null

    private fun utledEndretSluttdatoHistorikkDTO(
        sluttdatoErEndret: Boolean,
        forrigeDeltakelseRevisjon: DeltakelseDAO?,
        nåværendeDeltakelseRevisjon: DeltakelseDAO,
    ) = if (sluttdatoErEndret) {
        EndretSluttdatoHistorikk(
            gammelSluttdato = forrigeDeltakelseRevisjon?.getTom(),
            nySluttdato = nåværendeDeltakelseRevisjon.getTom()!! // Sluttdato kan ikke være null ved endring
        )
    } else null

    private fun utledEndretStartdatoHistorikkDTO(
        startdatoErEndret: Boolean,
        forrigeDeltakelseRevisjon: DeltakelseDAO?,
        nåværendeDeltakelseRevisjon: DeltakelseDAO,
    ) = if (startdatoErEndret && forrigeDeltakelseRevisjon != null) {
        EndretStartdatoHistorikk(
            gammelStartdato = forrigeDeltakelseRevisjon.getFom(),
            nyStartdato = nåværendeDeltakelseRevisjon.getFom()
        )
    } else null

    private fun utledEndringstype(
        forrigeDeltakelseRevisjon: DeltakelseDAO?,
        startdatoErEndret: Boolean,
        deltakerMeldtUt: Boolean,
        sluttdatoErEndret: Boolean,
        soktTidspunktErEndret: Boolean,
    ) = when {
        // Dersom vi ikke har en tidligere revisjon, betyr det at dette er den første revisjonen for deltakelsen.
        // Vi tolker dette som at deltakelsen er opprettet og at deltakeren er meldt inn i programmet.
        forrigeDeltakelseRevisjon == null -> Endringstype.DELTAKER_MELDT_INN
        startdatoErEndret -> Endringstype.ENDRET_STARTDATO
        deltakerMeldtUt -> Endringstype.DELTAKER_MELDT_UT
        sluttdatoErEndret -> Endringstype.ENDRET_SLUTTDATO
        soktTidspunktErEndret -> Endringstype.DELTAKER_HAR_SØKT_YTELSE
        else -> Endringstype.UKJENT
    }

    private fun håndterFlereEndringerISammeRevisjon(endredeFelter: List<String>, deltakelseId: UUID) {
        if (endredeFelter.size >= 2) {
            // Bytt ut siste komma med " og " om det er to eller flere elementer
            val felterTekst = endredeFelter.joinToString(separator = " og ")
            throw UnsupportedOperationException("Deltakelse med id $deltakelseId har endret $felterTekst i samme revisjon. Dette er uvanlig.")
        }
    }

    data class HistorikkEndring(
        val endringstype: Endringstype,
        val endretStartdatoData: EndretStartdatoHistorikk?,
        val deltakerMeldtUtData: DeltakerMeldtUtHistorikk?,
        val endretSluttdatoData: EndretSluttdatoHistorikk?,
        val søktTidspunktSatt: SøktTidspunktHistorikk?,
    )
}
