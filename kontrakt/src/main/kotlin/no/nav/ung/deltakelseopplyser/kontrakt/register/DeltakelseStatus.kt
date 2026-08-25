package no.nav.ung.deltakelseopplyser.kontrakt.register

/**
 * Status for en deltakelse i ungdomsprogrammet, utledet fra periodens sluttdato(er).
 *
 * - [LØPENDE]: Ingen sluttdato er satt ennå (`tilOgMed == null`).
 * - [VIL_AVSLUTTES]: Sluttdato er satt, men ligger frem i tid.
 * - [AVSLUTTET]: Sluttdatoen har passert (dagen etter sluttdato regnes som avsluttet).
 */
enum class DeltakelseStatus {
    LØPENDE,
    VIL_AVSLUTTES,
    AVSLUTTET
}
