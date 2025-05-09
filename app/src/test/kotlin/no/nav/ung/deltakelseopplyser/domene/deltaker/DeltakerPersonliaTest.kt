package no.nav.ung.deltakelseopplyser.domene.deltaker

import no.nav.pdl.generated.hentperson.Navn
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerPersonalia.Companion.PROGRAM_OPPSTART_DATO_PROPERTY
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class DeltakerPersonaliaTest {

    private fun lagDeltakerPersonalia(fødselsdato: LocalDate) = DeltakerPersonalia(
        id = UUID.randomUUID(),
        deltakerIdent = "12345678901",
        navn = Navn("Ola", null, "Nordmann"),
        fødselsdato = fødselsdato
    )

    @BeforeEach
    fun clearSystemProperty() {
        System.clearProperty(PROGRAM_OPPSTART_DATO_PROPERTY)
    }

    @Test
    fun `uten system-egenskap skal førsteMuligeInnmeldingsdato være 18 år + 1 måned etter fødselsdato`() {
        val fødselsdato = LocalDate.of(2000, 1, 15)
        assertEquals(
            LocalDate.of(2018, 2, 15),
            lagDeltakerPersonalia(fødselsdato).førsteMuligeInnmeldingsdato
        )
    }

    @Test
    fun `med system-egenskap senere enn aldersdato skal førsteMuligeInnmeldingsdato override`() {
        val fødselsdato = LocalDate.of(2000, 1, 15)
        System.setProperty(PROGRAM_OPPSTART_DATO_PROPERTY, "2025-08-01")
        assertEquals(
            LocalDate.of(2025, 8, 1),
            lagDeltakerPersonalia(fødselsdato).førsteMuligeInnmeldingsdato
        )
    }

    @Test
    fun `med system-egenskap tidligere enn aldersdato skal aldersdato vinne for førsteMuligeInnmelding`() {
        val fødselsdato = LocalDate.of(2000, 1, 15)
        System.setProperty(PROGRAM_OPPSTART_DATO_PROPERTY, "2017-01-01")
        assertEquals(
            LocalDate.of(2018, 2, 15),
            lagDeltakerPersonalia(fødselsdato).førsteMuligeInnmeldingsdato
        )
    }

    @Test
    fun `uten system-egenskap skal sisteMuligeInnmeldingsdato være fødselsdato + 29 år`() {
        val fødselsdato = LocalDate.of(1996, 5, 10)
        assertEquals(
            LocalDate.of(2025, 5, 10),
            lagDeltakerPersonalia(fødselsdato).sisteMuligeInnmeldingsdato
        )
    }

    @Test
    fun `med system-egenskap senere enn sisteMuligeInnmeldingsdato skal override`() {
        val fødselsdato = LocalDate.of(1996, 5, 10)
        System.setProperty(PROGRAM_OPPSTART_DATO_PROPERTY, "2025-08-01")
        assertEquals(
            LocalDate.of(2025, 8, 1),
            lagDeltakerPersonalia(fødselsdato).sisteMuligeInnmeldingsdato
        )
    }

    @Test
    fun `med system-egenskap tidligere enn sisteMuligeInnmeldingsdato skal alder vinne`() {
        val fødselsdato = LocalDate.of(1996, 5, 10)
        System.setProperty(PROGRAM_OPPSTART_DATO_PROPERTY, "2025-01-01")
        assertEquals(
            LocalDate.of(2025, 5, 10),
            lagDeltakerPersonalia(fødselsdato).sisteMuligeInnmeldingsdato
        )
    }
}
