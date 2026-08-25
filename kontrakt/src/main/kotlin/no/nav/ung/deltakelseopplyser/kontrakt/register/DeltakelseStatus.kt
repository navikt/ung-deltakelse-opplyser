package no.nav.ung.deltakelseopplyser.kontrakt.register

import java.time.LocalDate

/**
 * Status for en deltakelse i ungdomsprogrammet, utledet fra `tilOgMed` og `periodeMaksDato`.
 *
 * - [LØPENDE]: `tilOgMed` er ikke satt, og `periodeMaksDato` er ikke passert
 *   (dvs. dagens dato eller frem i tid).
 * - [VIL_AVSLUTTES]: `tilOgMed` er eksplisitt satt, og denne datoen er ikke passert
 *   (dagens dato eller frem i tid).
 * - [AVSLUTTET]: den effektive sluttdatoen (`tilOgMed` hvis satt, ellers `periodeMaksDato` som
 *   fallback) har passert, dvs. er før dagens dato. Dagens dato regnes altså ikke som avsluttet
 *   — status blir først [AVSLUTTET] dagen etter sluttdatoen.
 */
enum class DeltakelseStatus {
    LØPENDE,
    VIL_AVSLUTTES,
    AVSLUTTET;

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
                effektivSluttdato.isBefore(LocalDate.now()) -> AVSLUTTET
                tilOgMed != null -> VIL_AVSLUTTES
                else -> LØPENDE
            }
        }
    }
}
