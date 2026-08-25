package no.nav.ung.deltakelseopplyser.kontrakt.register

import java.time.LocalDate

/**
 * Status for en deltakelse i ungdomsprogrammet, utledet fra `tilOgMed` og `periodeMaksDato`.
 *
 * - [AKTIV]: `tilOgMed` er ikke satt, og `periodeMaksDato` er ikke passert
 *   (dvs. dagens dato eller frem i tid).
 * - [VIL_AVSLUTTES]: `tilOgMed` er eksplisitt satt, og denne datoen er ikke passert
 *   (dagens dato eller frem i tid).
 * - [IKKE_AKTIV]: den effektive sluttdatoen (`tilOgMed` hvis satt, ellers `periodeMaksDato` som
 *   fallback) har passert, dvs. er før dagens dato. Dagens dato regnes altså ikke som passert
 *   — status blir først [IKKE_AKTIV] dagen etter sluttdatoen.
 *
 * OBS: status er ikke en lagret/persistert tilstand, men beregnes på nytt hver gang den leses,
 * ut fra de til enhver tid gjeldende datoene. Den kan derfor endre seg over tid — også fra
 * [IKKE_AKTIV] tilbake til [AKTIV] eller [VIL_AVSLUTTES] — dersom `tilOgMed`/`periodeMaksDato`
 * endres i ettertid. Ikke cache denne verdien; hent status på nytt ved behov.
 */
enum class DeltakelseStatus {
    AKTIV,
    VIL_AVSLUTTES,
    IKKE_AKTIV;

    companion object {
        /**
         * Felles utledningslogikk brukt av alle DTO-er som eksponerer et statusfelt
         * (f.eks. [DeltakelseDTO] og `DeltakelsePeriodeDTO`), slik at status alltid er
         * konsistent utledet og aldri kan settes til en verdi som ikke stemmer med
         * de underliggende datoene.
         */
        fun utledFra(tilOgMed: LocalDate?, periodeMaksDato: LocalDate): DeltakelseStatus {
            val effektivSluttdato = tilOgMed ?: periodeMaksDato
            return when {
                effektivSluttdato.isBefore(LocalDate.now()) -> IKKE_AKTIV
                tilOgMed != null -> VIL_AVSLUTTES
                else -> AKTIV
            }
        }
    }
}
