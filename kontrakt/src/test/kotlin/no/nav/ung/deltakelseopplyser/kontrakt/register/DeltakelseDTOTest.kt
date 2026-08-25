package no.nav.ung.deltakelseopplyser.kontrakt.register

import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DeltakelseDTOTest {

    private val iDag: LocalDate = LocalDate.now()

    private fun deltakelse(
        tilOgMed: LocalDate? = null,
        periodeMaksDato: LocalDate = iDag.plusYears(1),
    ) = DeltakelseDTO(
        deltaker = DeltakerDTO(deltakerIdent = "12345678910"),
        fraOgMed = iDag.minusYears(1),
        tilOgMed = tilOgMed,
        periodeMaksDato = periodeMaksDato,
    )

    @Test
    fun `status er AKTIV når tilOgMed ikke er satt og periodeMaksDato ikke er passert`() {
        val dto = deltakelse(tilOgMed = null, periodeMaksDato = iDag.plusDays(1))
        assertThat(dto.status).isEqualTo(DeltakelseStatus.AKTIV)
    }

    @Test
    fun `status er AKTIV når tilOgMed ikke er satt og periodeMaksDato er i dag`() {
        val dto = deltakelse(tilOgMed = null, periodeMaksDato = iDag)
        assertThat(dto.status).isEqualTo(DeltakelseStatus.AKTIV)
    }

    @Test
    fun `status er VIL_AVSLUTTES når tilOgMed er satt frem i tid`() {
        val dto = deltakelse(tilOgMed = iDag.plusDays(1))
        assertThat(dto.status).isEqualTo(DeltakelseStatus.VIL_AVSLUTTES)
    }

    @Test
    fun `status er VIL_AVSLUTTES når tilOgMed er i dag`() {
        val dto = deltakelse(tilOgMed = iDag)
        assertThat(dto.status).isEqualTo(DeltakelseStatus.VIL_AVSLUTTES)
    }

    @Test
    fun `status er IKKE_AKTIV når tilOgMed har passert`() {
        val dto = deltakelse(tilOgMed = iDag.minusDays(1))
        assertThat(dto.status).isEqualTo(DeltakelseStatus.IKKE_AKTIV)
    }

    @Test
    fun `status er IKKE_AKTIV når tilOgMed ikke er satt men periodeMaksDato har passert`() {
        val dto = deltakelse(tilOgMed = null, periodeMaksDato = iDag.minusDays(1))
        assertThat(dto.status).isEqualTo(DeltakelseStatus.IKKE_AKTIV)
    }
}
