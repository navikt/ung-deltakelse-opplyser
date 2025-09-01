package no.nav.ung.deltakelseopplyser.integration.nom.api

import no.nav.nom.generated.enums.OrgEnhetsType
import no.nav.nom.generated.hentressurser.OrgEnhet
import no.nav.nom.generated.hentressurser.RessursOrgTilknytning
import no.nav.ung.deltakelseopplyser.integration.nom.api.domene.RessursOrgTilknytningUtils.harRelevantPeriode
import no.nav.ung.deltakelseopplyser.integration.nom.api.domene.RessursOrgTilknytningUtils.ikkeErFremtidig
import no.nav.ung.deltakelseopplyser.integration.nom.api.domene.RessursOrgTilknytningUtils.ikkeErHistorisk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class RessursOrgTilknytningUtilTest {
    private companion object {
        private fun mockOrgTilknytning(
            gyldigFom: String,
            gyldigTom: String? = null,
        ): RessursOrgTilknytning {
            return RessursOrgTilknytning(
                gyldigFom = gyldigFom,
                gyldigTom = gyldigTom,
                orgEnhet = OrgEnhet(
                    id = UUID.randomUUID().toString(),
                    remedyEnhetId = UUID.randomUUID().toString(),
                    navn = "Testenhet",
                    gyldigFom = gyldigFom,
                    gyldigTom = gyldigTom,
                    orgEnhetsType = OrgEnhetsType.NAV_KONTOR
                )
            )
        }
    }

    @Test
    fun `skal returnere enhet med relevant periode når gyldigTom er null`() {
        val orgTilknytning = mockOrgTilknytning(gyldigFom = "2020-01-01", gyldigTom = null)
        assertThat(orgTilknytning.ikkeErHistorisk()).isTrue
        assertThat(orgTilknytning.ikkeErFremtidig()).isTrue
        assertThat(orgTilknytning.harRelevantPeriode()).isTrue
    }

    @Test
    fun `skal returnere gyldig periode når gyldigTom er i fremtiden`() {
        val tomorrow = LocalDate.now().plusDays(1).toString()
        val orgTilknytning = mockOrgTilknytning(gyldigFom = "2020-01-01", gyldigTom = tomorrow)
        assertThat(orgTilknytning.ikkeErFremtidig()).isTrue
        assertThat(orgTilknytning.ikkeErHistorisk()).isTrue
        assertThat(orgTilknytning.harRelevantPeriode()).isTrue
    }

    @Test
    fun `skal returnere ugyldig periode når gyldigTom er i fortiden`() {
        val yesterday = LocalDate.now().minusDays(1).toString()
        val orgTilknytning = mockOrgTilknytning(gyldigFom = "2020-01-01", gyldigTom = yesterday)
        assertThat(orgTilknytning.ikkeErFremtidig()).isFalse
        assertThat(orgTilknytning.ikkeErHistorisk()).isFalse
        assertThat(orgTilknytning.harRelevantPeriode()).isFalse
    }

    @Test
    fun `ikkeErHistorisk skal returnere true når gyldigFom ikke er tom og gyldigFom er før eller lik dagens dato`() {
        val today = LocalDate.now().toString()
        val orgTilknytning = mockOrgTilknytning(gyldigFom = today, gyldigTom = null)
        assertThat(orgTilknytning.ikkeErHistorisk()).isTrue
        assertThat(orgTilknytning.ikkeErFremtidig()).isTrue
        assertThat(orgTilknytning.harRelevantPeriode()).isTrue
    }

    @Test
    fun `ikkeErFremtidig skal returnere true når gyldigTom ikke er null og gyldigTom er etter eller lik dagens dato`() {
        val tomorrow = LocalDate.now().plusDays(1).toString()
        val orgTilknytning = mockOrgTilknytning(gyldigFom = "2020-01-01", gyldigTom = tomorrow)
        assertThat(orgTilknytning.ikkeErFremtidig()).isTrue
        assertThat(orgTilknytning.ikkeErHistorisk()).isTrue
        assertThat(orgTilknytning.harRelevantPeriode()).isTrue
    }

    @Test
    fun `skal returnere true når periode er aktiv akkurat nå`() {
        val yesterday = LocalDate.now().minusDays(1).toString()
        val tomorrow = LocalDate.now().plusDays(1).toString()
        val orgTilknytning = mockOrgTilknytning(gyldigFom = yesterday, gyldigTom = tomorrow)
        assertThat(orgTilknytning.ikkeErFremtidig()).isTrue
        assertThat(orgTilknytning.ikkeErHistorisk()).isTrue
        assertThat(orgTilknytning.harRelevantPeriode()).isTrue
    }
}
