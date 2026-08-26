package no.nav.ung.deltakelseopplyser.kontrakt.register

import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Status for en deltakelse i ungdomsprogrammet, utledet fra `søktTidspunkt`, `tilOgMed` og
 * `periodeMaksDato`.
 *
 * - [IKKE_STARTET]: `søktTidspunkt` er ikke satt ennå, dvs. bruker har ikke søkt om ytelsen.
 *   Dette trumfer periodedatoene — en deltakelse uten søknad regnes alltid som [IKKE_STARTET],
 *   selv i det usannsynlige tilfellet at `periodeMaksDato` allerede skulle ha passert.
 * - [AKTIV]: `søktTidspunkt` er satt, og den effektive sluttdatoen (`tilOgMed` hvis satt, ellers
 *   `periodeMaksDato` som fallback) er ikke passert (dvs. dagens dato eller frem i tid).
 * - [IKKE_AKTIV]: `søktTidspunkt` er satt, og den effektive sluttdatoen har passert, dvs. er før
 *   dagens dato. Dagens dato regnes altså ikke som passert — status blir først [IKKE_AKTIV]
 *   dagen etter sluttdatoen.
 *
 * OBS: status er ikke en lagret/persistert tilstand, men beregnes på nytt hver gang den leses,
 * ut fra de til enhver tid gjeldende feltene. Den kan derfor endre seg over tid — også fra
 * [IKKE_AKTIV] tilbake til [AKTIV] — dersom `tilOgMed`/`periodeMaksDato` endres i ettertid.
 * Ikke cache denne verdien; hent status på nytt ved behov.
 */
enum class DeltakelseStatus {
    IKKE_STARTET,
    AKTIV,
    IKKE_AKTIV;

    companion object {
        /**
         * Felles utledningslogikk brukt av alle DTO-er som eksponerer et statusfelt
         * (f.eks. [DeltakelseDTO] og `DeltakelsePeriodeDTO`), slik at status alltid er
         * konsistent utledet og aldri kan settes til en verdi som ikke stemmer med
         * de underliggende feltene.
         */
        fun utledFra(søktTidspunkt: ZonedDateTime?, tilOgMed: LocalDate?, periodeMaksDato: LocalDate): DeltakelseStatus {
            // søktTidspunkt trumfer periodedatoene: uten søknad er deltakelsen ikke startet,
            // uansett hva periodedatoene skulle tilsi.
            if (søktTidspunkt == null) {
                return IKKE_STARTET
            }
            val effektivSluttdato = tilOgMed ?: periodeMaksDato
            return if (effektivSluttdato.isBefore(LocalDate.now())) IKKE_AKTIV else AKTIV
        }
    }
}
