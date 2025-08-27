package no.nav.ung.deltakelseopplyser.integration.nom.api

import no.nav.nom.generated.hentressurser.OrgEnhet
import java.time.LocalDate

object OrgEnhetUtils {
    fun OrgEnhet.ikkeErHistorisk(): Boolean {
        return gyldigFom.isNotBlank() && LocalDate.parse(gyldigFom).isBefore(LocalDate.now().plusDays(1))
    }

    fun OrgEnhet.ikkeErFremtidig(): Boolean {
        return gyldigTom.isNullOrBlank() || LocalDate.parse(gyldigTom).isAfter(LocalDate.now().minusDays(1))
    }

    fun OrgEnhet.harRelevantPeriode(): Boolean {
        return ikkeErHistorisk() && ikkeErFremtidig()
    }
}
