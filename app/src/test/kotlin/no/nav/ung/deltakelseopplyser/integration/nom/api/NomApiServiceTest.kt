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
    fun `skal returnere enhet med relevant periode når gyldigTom er null`() {
        val orgEnhet = mockEnhet(gyldigFom = "2020-01-01", gyldigTom = null)
        assertThat(orgEnhet.ikkeErHistorisk()).isTrue
        assertThat(orgEnhet.ikkeErFremtidig()).isTrue
        assertThat(orgEnhet.harRelevantPeriode()).isTrue
    }

    @Test
    fun `skal returnere gyldig periode når gyldigTom er i fremtiden`() {
        val tomorrow = LocalDate.now().plusDays(1).toString()
        val orgEnhet = mockEnhet(gyldigFom = "2020-01-01", gyldigTom = tomorrow)
        assertThat(orgEnhet.ikkeErFremtidig()).isTrue
        assertThat(orgEnhet.ikkeErHistorisk()).isTrue
        assertThat(orgEnhet.harRelevantPeriode()).isTrue
    }

    @Test
    fun `skal returnere ugyldig periode når gyldigTom er i fortiden`() {
        val yesterday = LocalDate.now().minusDays(1).toString()
        val orgEnhet = mockEnhet(gyldigFom = "2020-01-01", gyldigTom = yesterday)
        assertThat(orgEnhet.ikkeErFremtidig()).isFalse
        assertThat(orgEnhet.ikkeErHistorisk()).isFalse
        assertThat(orgEnhet.harRelevantPeriode()).isFalse
    }

    @Test
    fun `ikkeErHistorisk skal returnere true når gyldigFom ikke er tom og gyldigFom er før eller lik dagens dato`() {
        val today = LocalDate.now().toString()
        val orgEnhet = mockEnhet(gyldigFom = today, gyldigTom = null)
        assertThat(orgEnhet.ikkeErHistorisk()).isTrue
        assertThat(orgEnhet.ikkeErFremtidig()).isTrue
        assertThat(orgEnhet.harRelevantPeriode()).isTrue
    }

    @Test
    fun `ikkeErFremtidig skal returnere true når gyldigTom ikke er null og gyldigTom er etter eller lik dagens dato`() {
        val tomorrow = LocalDate.now().plusDays(1).toString()
        val orgEnhet = mockEnhet(gyldigFom = "2020-01-01", gyldigTom = tomorrow)
        assertThat(orgEnhet.ikkeErFremtidig()).isTrue
        assertThat(orgEnhet.ikkeErHistorisk()).isTrue
        assertThat(orgEnhet.harRelevantPeriode()).isTrue
    }

    @Test
    fun `skal returnere true når periode er aktiv akkurat nå`() {
        val yesterday = LocalDate.now().minusDays(1).toString()
        val tomorrow = LocalDate.now().plusDays(1).toString()
        val orgEnhet = mockEnhet(gyldigFom = yesterday, gyldigTom = tomorrow)
        assertThat(orgEnhet.ikkeErFremtidig()).isTrue
        assertThat(orgEnhet.ikkeErHistorisk()).isTrue
        assertThat(orgEnhet.harRelevantPeriode()).isTrue
    }
}
