package no.nav.ung.deltakelseopplyser.integration.nom.api

import no.nav.nom.generated.enums.OrgEnhetsType
import no.nav.nom.generated.hentressurser.OrgEnhet
import no.nav.ung.deltakelseopplyser.integration.nom.api.OrgEnhetUtils.harRelevantPeriode
import no.nav.ung.deltakelseopplyser.integration.nom.api.OrgEnhetUtils.ikkeErFremtidig
import no.nav.ung.deltakelseopplyser.integration.nom.api.OrgEnhetUtils.ikkeErHistorisk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class OrgEnhetTest {
    private companion object {
        private fun mockEnhet(
            gyldigFom: String,
            gyldigTom: String? = null,
        ): OrgEnhet {
            return OrgEnhet(
                id = UUID.randomUUID().toString(),
                remedyEnhetId = UUID.randomUUID().toString(),
                navn = "Testenhet",
                gyldigFom = gyldigFom,
                gyldigTom = gyldigTom,
                orgEnhetsType = OrgEnhetsType.NAV_KONTOR
            )
        }
    }

    @Test
    fun `Forventer at enhet med relevant periode når start er `() {
        val orgEnhet = mockEnhet(gyldigFom = "2020-01-01", gyldigTom = null)
        assertThat(orgEnhet.ikkeErHistorisk()).isTrue
        assertThat(orgEnhet.ikkeErFremtidig()).isTrue
        assertThat(orgEnhet.harRelevantPeriode()).isTrue
    }

    @Test
    fun `harGyldigPeriode should return true when gyldigTom is in the future`() {
        val tomorrow = LocalDate.now().plusDays(1).toString()
        val orgEnhet = mockEnhet(gyldigFom = "2020-01-01", gyldigTom = tomorrow)
        assertThat(orgEnhet.ikkeErFremtidig()).isTrue
        assertThat(orgEnhet.ikkeErHistorisk()).isTrue
        assertThat(orgEnhet.harRelevantPeriode()).isTrue
    }

    @Test
    fun `harGyldigPeriode should return false when gyldigTom is in the past`() {
        val yesterday = LocalDate.now().minusDays(1).toString()
        val orgEnhet = mockEnhet(gyldigFom = "2020-01-01", gyldigTom = yesterday)
        assertThat(orgEnhet.ikkeErFremtidig()).isFalse
        assertThat(orgEnhet.ikkeErHistorisk()).isFalse
        assertThat(orgEnhet.harRelevantPeriode()).isFalse
    }

    @Test
    fun `ikkeErHistorisk should return true when gyldigFom is not blank and gyldigFom is before or equal to today`() {
        val today = LocalDate.now().toString()
        val orgEnhet = mockEnhet(gyldigFom = today, gyldigTom = null)
        assertThat(orgEnhet.ikkeErHistorisk()).isTrue
        assertThat(orgEnhet.ikkeErFremtidig()).isTrue
        assertThat(orgEnhet.harRelevantPeriode()).isTrue
    }

    @Test
    fun `ikkeErFremtidig should return true when gyldigTom is not null and gyldigTom is after or equal to today`() {
        val tomorrow = LocalDate.now().plusDays(1).toString()
        val orgEnhet = mockEnhet(gyldigFom = "2020-01-01", gyldigTom = tomorrow)
        assertThat(orgEnhet.ikkeErFremtidig()).isTrue
        assertThat(orgEnhet.ikkeErHistorisk()).isTrue
        assertThat(orgEnhet.harRelevantPeriode()).isTrue
    }

    @Test
    fun `erPågående should return true when period is currently active`() {
        val yesterday = LocalDate.now().minusDays(1).toString()
        val tomorrow = LocalDate.now().plusDays(1).toString()
        val orgEnhet = mockEnhet(gyldigFom = yesterday, gyldigTom = tomorrow)
        assertThat(orgEnhet.ikkeErFremtidig()).isTrue
        assertThat(orgEnhet.ikkeErHistorisk()).isTrue
        assertThat(orgEnhet.harRelevantPeriode()).isTrue
    }
}
