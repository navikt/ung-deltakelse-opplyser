package no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles

enum class Oppgavetype(val mineSiderVarselTekst: String) {
    BEKREFT_ENDRET_PROGRAMPERIODE("Du har fått en oppgave om å bekrefte endret programperiode"),
    BEKREFT_AVVIK_REGISTERINNTEKT("Du har fått en oppgave om å bekrefte inntekten din"),
}
