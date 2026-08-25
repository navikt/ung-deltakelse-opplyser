package no.nav.ung.deltakelseopplyser.kontrakt.register

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DeltakelseStatusTest {

    private val iDag: LocalDate = LocalDate.now()

    @Test
    fun `utledFra returnerer AKTIV når tilOgMed er null og periodeMaksDato ikke er passert`() {
        assertThat(DeltakelseStatus.utledFra(tilOgMed = null, periodeMaksDato = iDag.plusDays(1)))
            .isEqualTo(DeltakelseStatus.AKTIV)
    }

    @Test
    fun `utledFra returnerer AKTIV når tilOgMed er null og periodeMaksDato er i dag`() {
        assertThat(DeltakelseStatus.utledFra(tilOgMed = null, periodeMaksDato = iDag))
            .isEqualTo(DeltakelseStatus.AKTIV)
    }

    @Test
    fun `utledFra returnerer VIL_AVSLUTTES når tilOgMed er satt frem i tid`() {
        assertThat(DeltakelseStatus.utledFra(tilOgMed = iDag.plusDays(1), periodeMaksDato = iDag.plusYears(1)))
            .isEqualTo(DeltakelseStatus.VIL_AVSLUTTES)
    }

    @Test
    fun `utledFra returnerer VIL_AVSLUTTES når tilOgMed er i dag`() {
        assertThat(DeltakelseStatus.utledFra(tilOgMed = iDag, periodeMaksDato = iDag.plusYears(1)))
            .isEqualTo(DeltakelseStatus.VIL_AVSLUTTES)
    }

    @Test
    fun `utledFra returnerer IKKE_AKTIV når tilOgMed har passert`() {
        assertThat(DeltakelseStatus.utledFra(tilOgMed = iDag.minusDays(1), periodeMaksDato = iDag.plusYears(1)))
            .isEqualTo(DeltakelseStatus.IKKE_AKTIV)
    }

    @Test
    fun `utledFra returnerer IKKE_AKTIV når tilOgMed er null og periodeMaksDato har passert`() {
        assertThat(DeltakelseStatus.utledFra(tilOgMed = null, periodeMaksDato = iDag.minusDays(1)))
            .isEqualTo(DeltakelseStatus.IKKE_AKTIV)
    }
}
