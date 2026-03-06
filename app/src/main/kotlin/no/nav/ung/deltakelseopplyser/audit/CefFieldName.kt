package no.nav.ung.deltakelseopplyser.audit

/**
 * Felter skal ikke brukes til andre verdier enn det som er avtalt med ArcSight-gjengen.
 */
enum class CefFieldName(val kode: String) {
    /** Tidspunkt for når hendelsen skjedde. */
    EVENT_TIME("end"),

    /** Brukeren som startet hendelsen (saksbehandler/veileder/...). */
    USER_ID("suid"),

    /** Bruker (søker/part/...) som har personopplysninger som blir berørt. */
    BERORT_BRUKER_ID("duid"),

    /** Det som blir bedt om: OK med både URLer og hendelsesnavn. */
    REQUEST("request"),

    /** Brukes til ABAC-ressurstype. */
    ABAC_RESOURCE_TYPE("requestContext"),

    /** Ekstra felt for ABAC-action (fordi det samme blir definert gjennom EventClassId). */
    ABAC_ACTION("act"),

    /**
     * Reservert til bruk for "Saksnummer". Det er godkjent med både eksternt saksnummer
     * og FagsakId, men førstnevnte er foretrukket. Denne skal unikt identifisere fagsaken.
     */
    SAKSNUMMER_VERDI("flexString1"),

    /** Reservert til bruk for "Saksnummer". */
    SAKSNUMMER_LABEL("flexString1Label"),

    /**
     * Reservert til bruk for "Behandling". Det er godkjent med både behandlingsUuid
     * og behandlingsId, men førstnevnte er foretrukket. Denne skal unikt identifisere behandlingen.
     */
    BEHANDLING_VERDI("flexString2"),

    /** Reservert til bruk for "Behandling". */
    BEHANDLING_LABEL("flexString2Label"),
}

