package no.nav.ung.deltakelseopplyser.kontrakt.register

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZonedDateTime

class DeltakelseStatusTest {

    private val iDag: LocalDate = LocalDate.now()
    private val søktIGår: ZonedDateTime = ZonedDateTime.now().minusDays(1)

    @Test
    fun `utledFra returnerer IKKE_STARTET når søktTidspunkt er null`() {
        assertThat(
            DeltakelseStatus.utledFra(
                søktTidspunkt = null,
                tilOgMed = null,
                periodeMaksDato = iDag.plusDays(1)
            )
        ).isEqualTo(DeltakelseStatus.IKKE_STARTET)
    }

    @Test
    fun `utledFra returnerer IKKE_STARTET når søktTidspunkt er null selv om periodeMaksDato har passert`() {
        assertThat(
            DeltakelseStatus.utledFra(
                søktTidspunkt = null,
                tilOgMed = null,
                periodeMaksDato = iDag.minusDays(1)
            )
        ).isEqualTo(DeltakelseStatus.IKKE_STARTET)
    }

    @Test
    fun `utledFra returnerer AKTIV når søktTidspunkt er satt, tilOgMed er null og periodeMaksDato ikke er passert`() {
        assertThat(
            DeltakelseStatus.utledFra(
                søktTidspunkt = søktIGår,
                tilOgMed = null,
                periodeMaksDato = iDag.plusDays(1)
            )
        ).isEqualTo(DeltakelseStatus.AKTIV)
    }

    @Test
    fun `utledFra returnerer AKTIV når søktTidspunkt er satt og periodeMaksDato er i dag`() {
        assertThat(
            DeltakelseStatus.utledFra(
                søktTidspunkt = søktIGår,
                tilOgMed = null,
                periodeMaksDato = iDag
            )
        ).isEqualTo(DeltakelseStatus.AKTIV)
    }

    @Test
    fun `utledFra returnerer AKTIV når søktTidspunkt er satt og tilOgMed er frem i tid`() {
        assertThat(
            DeltakelseStatus.utledFra(
                søktTidspunkt = søktIGår,
                tilOgMed = iDag.plusDays(1),
                periodeMaksDato = iDag.plusYears(1)
            )
        ).isEqualTo(DeltakelseStatus.AKTIV)
    }

    @Test
    fun `utledFra returnerer AKTIV når søktTidspunkt er satt og tilOgMed er i dag`() {
        assertThat(
            DeltakelseStatus.utledFra(
                søktTidspunkt = søktIGår,
                tilOgMed = iDag,
                periodeMaksDato = iDag.plusYears(1)
            )
        ).isEqualTo(DeltakelseStatus.AKTIV)
    }

    @Test
    fun `utledFra returnerer IKKE_AKTIV når søktTidspunkt er satt og tilOgMed har passert`() {
        assertThat(
            DeltakelseStatus.utledFra(
                søktTidspunkt = søktIGår,
                tilOgMed = iDag.minusDays(1),
                periodeMaksDato = iDag.plusYears(1)
            )
        ).isEqualTo(DeltakelseStatus.IKKE_AKTIV)
    }

    @Test
    fun `utledFra returnerer IKKE_AKTIV når søktTidspunkt er satt, tilOgMed er null og periodeMaksDato har passert`() {
        assertThat(
            DeltakelseStatus.utledFra(
                søktTidspunkt = søktIGår,
                tilOgMed = null,
                periodeMaksDato = iDag.minusDays(1)
            )
        ).isEqualTo(DeltakelseStatus.IKKE_AKTIV)
    }
}
