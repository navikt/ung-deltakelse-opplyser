package no.nav.ung.deltakelseopplyser.domene.register.historikk

import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseDAO
import no.nav.ung.deltakelseopplyser.domene.register.ForlengetPeriodeBeregner
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
        val sluttdatoSlettet = forrigeSluttdato != null && nåværendeSluttdato == null
        val sluttdatoErEndret =
            forrigeSluttdato != null && nåværendeSluttdato != null && forrigeSluttdato != nåværendeSluttdato
        val deltakelseErFjernet = nåværendeDeltakelseRevisjon.erSlettet && forrigeDeltakelseRevisjon?.erSlettet != true

        val soktTidspunktErEndret =
            forrigeDeltakelseRevisjon?.søktTidspunkt != nåværendeDeltakelseRevisjon.søktTidspunkt

        val periodeForlenget =
            nåværendeDeltakelseRevisjon.harForlengetPeriode && forrigeDeltakelseRevisjon?.harForlengetPeriode != true

        // Lag liste med navn på de feltene som faktisk endret seg
        val endredeFelter = listOfNotNull(
            "startdato".takeIf { startdatoErEndret },
            "sluttdatoSatt".takeIf { deltakerMeldtUt },
            "sluttdatoSlettet".takeIf { sluttdatoSlettet },
            "sluttdatoEndret".takeIf { sluttdatoErEndret && !periodeForlenget },
            "søktTidspunkt".takeIf { soktTidspunktErEndret },
            "deltakelseFjernet".takeIf { deltakelseErFjernet },
            "forlengetPeriode".takeIf { periodeForlenget }
        )

        håndterFlereEndringerISammeRevisjon(endredeFelter, nåværendeDeltakelseRevisjon.id)

        return HistorikkEndring(
            endringstype = utledEndringstype(
                forrigeDeltakelseRevisjon,
                startdatoErEndret,
                deltakerMeldtUt,
                sluttdatoSlettet,
                sluttdatoErEndret,
                soktTidspunktErEndret,
                deltakelseErFjernet,
                periodeForlenget
            ),

            endretStartdatoData = utledEndretStartdatoHistorikkDTO(
                startdatoErEndret,
                forrigeDeltakelseRevisjon,
                nåværendeDeltakelseRevisjon
            ),

            deltakerMeldtUtData = utledDeltakerMeldtUtHistorikk(deltakerMeldtUt, nåværendeDeltakelseRevisjon),

            endretSluttdatoData = utledEndretSluttdatoHistorikkDTO(
                sluttdatoErEndret && !periodeForlenget,
                forrigeDeltakelseRevisjon,
                nåværendeDeltakelseRevisjon
            ),

            sluttdatoSlettetData = if (sluttdatoSlettet) {
                SluttdatoSlettetHistorikk(
                    slettetSluttdato = requireNotNull(forrigeSluttdato) {
                        "Forrige sluttdato kan ikke være null ved sletting av sluttdato"
                    }
                )
            } else null,

            søktTidspunktSatt = utledSøktTidspunktHistorikkDTO(soktTidspunktErEndret, nåværendeDeltakelseRevisjon),

            deltakelseFjernetData = if (deltakelseErFjernet) DeltakelseFjernetHistorikk(
                forrigeStartdato = forrigeDeltakelseRevisjon!!.getFom(),
                forrigeSluttdato = forrigeSluttdato
            ) else null,

            forlengetPeriodeData = utledForlengetPeriodeHistorikk(periodeForlenget, nåværendeDeltakelseRevisjon)
        )
    }

    private fun utledDeltakerMeldtUtHistorikk(
        deltakerMeldtUt: Boolean,
        nåværendeDeltakelseRevisjon: DeltakelseDAO,
    ): DeltakerMeldtUtHistorikk? {
        return if (deltakerMeldtUt) {
            DeltakerMeldtUtHistorikk(
                utmeldingDato = requireNotNull(nåværendeDeltakelseRevisjon.getTom()) {
                    "Sluttdato kan ikke være null ved utmelding"
                }
            )
        } else null
    }

    private fun utledSøktTidspunktHistorikkDTO(
        soktTidspunktErEndret: Boolean,
        nåværendeDeltakelseRevisjon: DeltakelseDAO,
    ) = if (soktTidspunktErEndret) {
        SøktTidspunktHistorikk(
            søktTidspunktSatt = soktTidspunktErEndret,
            søktTidspunkt = requireNotNull(nåværendeDeltakelseRevisjon.søktTidspunkt) {
                "Søkt tidspunkt kan ikke være null ved endring av søkt tidspunkt"
            }
        )
    } else null

    private fun utledEndretSluttdatoHistorikkDTO(
        sluttdatoErEndret: Boolean,
        forrigeDeltakelseRevisjon: DeltakelseDAO?,
        nåværendeDeltakelseRevisjon: DeltakelseDAO,
    ) = if (sluttdatoErEndret) {
        EndretSluttdatoHistorikk(
            gammelSluttdato = requireNotNull(forrigeDeltakelseRevisjon!!.getTom()) {
                "Forrige sluttdato kan ikke være null ved endring av sluttdato"
            },
            nySluttdato = requireNotNull(nåværendeDeltakelseRevisjon.getTom()) {
                "Ny sluttdato kan ikke være null ved endring av sluttdato"
            }
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
        sluttdatoSlettet: Boolean,
        sluttdatoErEndret: Boolean,
        soktTidspunktErEndret: Boolean,
        deltakelseErFjernet: Boolean,
        periodeForlenget: Boolean,
    ) = when {
        // Dersom vi ikke har en tidligere revisjon, betyr det at dette er den første revisjonen for deltakelsen.
        // Vi tolker dette som at deltakelsen er opprettet og at deltakeren er meldt inn i programmet.
        forrigeDeltakelseRevisjon == null -> Endringstype.DELTAKER_MELDT_INN
        startdatoErEndret -> Endringstype.ENDRET_STARTDATO
        deltakerMeldtUt -> Endringstype.DELTAKER_MELDT_UT
        sluttdatoSlettet -> Endringstype.SLUTTDATO_SLETTET
        periodeForlenget -> Endringstype.FORLENGET_PERIODE
        sluttdatoErEndret -> Endringstype.ENDRET_SLUTTDATO
        soktTidspunktErEndret -> Endringstype.DELTAKER_HAR_SØKT_YTELSE
        deltakelseErFjernet -> Endringstype.DELTAKELSE_FJERNET
        else -> Endringstype.UKJENT
    }

    private fun håndterFlereEndringerISammeRevisjon(endredeFelter: List<String>, deltakelseId: UUID) {
        if (endredeFelter.size >= 2) {
            // Bytt ut siste komma med " og " om det er to eller flere elementer
            val felterTekst = endredeFelter.joinToString(separator = " og ")
            throw UnsupportedOperationException("Deltakelse med id $deltakelseId har endret $felterTekst i samme revisjon. Dette er uvanlig.")
        }
    }

    private fun utledForlengetPeriodeHistorikk(
        periodeForlenget: Boolean,
        nåværendeDeltakelseRevisjon: DeltakelseDAO,
    ): ForlengetPeriodeHistorikk? {
        if (!periodeForlenget) return null
        val periode = ForlengetPeriodeBeregner.beregn(nåværendeDeltakelseRevisjon.getFom(), nåværendeDeltakelseRevisjon.harForlengetPeriode)
        return ForlengetPeriodeHistorikk(
            forlengetFraOgMed = periode.fraOgMed,
            forlengetTilOgMed = periode.tilOgMed
        )
    }

    data class HistorikkEndring(
        val endringstype: Endringstype,
        val endretStartdatoData: EndretStartdatoHistorikk?,
        val deltakerMeldtUtData: DeltakerMeldtUtHistorikk?,
        val endretSluttdatoData: EndretSluttdatoHistorikk?,
        val sluttdatoSlettetData: SluttdatoSlettetHistorikk?,
        val søktTidspunktSatt: SøktTidspunktHistorikk?,
        val deltakelseFjernetData: DeltakelseFjernetHistorikk?,
        val forlengetPeriodeData: ForlengetPeriodeHistorikk?,
    )
}
