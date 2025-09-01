package no.nav.ung.deltakelseopplyser.integration.nom.api

import no.nav.nom.generated.hentressurser.OrgEnhet
import java.time.LocalDate

object OrgEnhetUtils {
    fun OrgEnhet.ikkeErHistorisk(): Boolean {
        val gyldigFra = if (gyldigFom.isNotBlank()) LocalDate.parse(gyldigFom) else null
        val gyldigTil = if (!gyldigTom.isNullOrBlank()) LocalDate.parse(gyldigTom) else null
        val idag = LocalDate.now()

        // Hvis gyldigFra dato eksisterer og er etter idag, er den fremtidig
        if (gyldigFra != null && gyldigFra.isAfter(idag)) {
            return true
        }

        // Hvis gyldigTil dato eksisterer og er før idag, er den historisk
        if (gyldigTil != null && gyldigTil.isBefore(idag)) {
            return false
        }

        // Hvis gyldigTil er null eller etter idag, og gyldigFra er før eller lik idag, er den gyldig nå
        return gyldigFra != null
    }

    fun OrgEnhet.ikkeErFremtidig(): Boolean {
        return gyldigTom.isNullOrBlank() || LocalDate.parse(gyldigTom).isAfter(LocalDate.now().minusDays(1))
    }

    fun OrgEnhet.harRelevantPeriode(): Boolean {
        return ikkeErHistorisk() && ikkeErFremtidig()
    }

    /**
     * Sjekker om enheten var gyldig på et spesifikt tidspunkt.
     */
    fun OrgEnhet.erGyldigPåTidspunkt(tidspunkt: LocalDate): Boolean {
        val gyldigFra =  LocalDate.parse(gyldigFom)
        val gyldigTil = if (!gyldigTom.isNullOrBlank()) LocalDate.parse(gyldigTom) else null

        // Enheten må ha startet før eller på tidspunktet
        val startetFørEllerPå = !gyldigFra.isAfter(tidspunkt)

        // Enheten må ikke ha sluttet før tidspunktet
        val ikkeSlutetFør = gyldigTil == null || !gyldigTil.isBefore(tidspunkt)

        return startetFørEllerPå && ikkeSlutetFør
    }
}
