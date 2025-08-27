package no.nav.ung.deltakelseopplyser.integration.nom.api

import no.nav.nom.generated.hentressurser.RessursOrgTilknytning
import java.time.LocalDate

object RessursOrgTilknytningUtils {
    fun RessursOrgTilknytning.ikkeErHistorisk(): Boolean {
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

    fun RessursOrgTilknytning.ikkeErFremtidig(): Boolean {
        return gyldigTom.isNullOrBlank() || LocalDate.parse(gyldigTom).isAfter(LocalDate.now().minusDays(1))
    }

    fun RessursOrgTilknytning.harRelevantPeriode(): Boolean {
        return ikkeErHistorisk() && ikkeErFremtidig()
    }
}
