package no.nav.ung.deltakelseopplyser.domene.register

import no.nav.fpsak.tidsserie.LocalDateInterval
import java.time.LocalDate

/**
 * Beregner forlenget periode for en deltakelse i ungdomsprogrammet.
 *
 * Programmet har en grunnperiode på 260 virkedager. Ved forlenget periode legges det til
 * 40 nye virkedager kant i kant etter grunnperioden.
 *
 * Helger (lørdag/søndag) telles ikke med – kun virkedager (mandag–fredag).
 */
object ForlengetPeriodeBeregner {

    private const val GRUNNPERIODE_VIRKEDAGER = 260
    private const val FORLENGET_PERIODE_VIRKEDAGER = 40
    private const val TOTALT_VIRKEDAGER_MED_FORLENGET_PERIODE = GRUNNPERIODE_VIRKEDAGER + FORLENGET_PERIODE_VIRKEDAGER

    data class ForlengetPeriode(
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate,
    )

    fun beregn(deltakelseStartdato: LocalDate, harForlengetPeriode: Boolean = false): ForlengetPeriode {
        val antallVirkedager = if (harForlengetPeriode) TOTALT_VIRKEDAGER_MED_FORLENGET_PERIODE else GRUNNPERIODE_VIRKEDAGER
        val fraOgMed = deltakelseStartdato
        val tilOgMed = finnSluttdatoForVirkedager(fraOgMed, antallVirkedager)
        return ForlengetPeriode(
            fraOgMed = fraOgMed,
            tilOgMed = tilOgMed,
        )
    }

    /**
     * Finner sluttdatoen slik at perioden [fraOgMed, sluttdato] inneholder nøyaktig
     * [antallVirkedager] virkedager (mandag–fredag).
     *
     * Bruker [LocalDateInterval.adjustWeekendToMonday] og [LocalDateInterval.nextWorkday]
     * for å hoppe over helger.
     */
    internal fun finnSluttdatoForVirkedager(fraOgMed: LocalDate, antallVirkedager: Int): LocalDate {
        require(antallVirkedager >= 1)
        var dato = LocalDateInterval.adjustWeekendToMonday(fraOgMed)
        repeat(antallVirkedager - 1) {
            dato = LocalDateInterval.nextWorkday(dato)
        }
        return dato
    }
}

