package no.nav.ung.deltakelseopplyser.domene.register

import no.nav.fpsak.tidsserie.LocalDateInterval
import java.time.LocalDate

/**
 * Beregner utvidet kvoteperiode for en deltakelse i ungdomsprogrammet.
 *
 * Programmet har en grunnkvote på 260 virkedager. Ved utvidet kvote legges det til
 * 40 nye virkedager kant i kant etter grunnkvoten.
 *
 * Helger (lørdag/søndag) telles ikke med – kun virkedager (mandag–fredag).
 */
object UtvidetKvoteBeregner {

    private const val GRUNNKVOTE_VIRKEDAGER = 260
    private const val UTVIDET_KVOTE_VIRKEDAGER = 40
    private const val TOTALT_VIRKEDAGER_MED_UTVIDET_KVOTE = GRUNNKVOTE_VIRKEDAGER + UTVIDET_KVOTE_VIRKEDAGER

    data class UtvidetKvotePeriode(
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate,
    )

    fun beregn(deltakelse: DeltakelseDAO): UtvidetKvotePeriode {
        val fraOgMed = deltakelse.getFom()
        val tilOgMed = finnSluttdatoForVirkedager(fraOgMed, TOTALT_VIRKEDAGER_MED_UTVIDET_KVOTE)
        return UtvidetKvotePeriode(
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
