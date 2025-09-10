package no.nav.ung.deltakelseopplyser.domene.deltaker

import no.nav.pdl.generated.hentperson.Navn
import no.nav.ung.deltakelseopplyser.utils.FødselsnummerGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.test.assertTrue

internal class DeltakerPersonaliaTest {

    private fun lagDeltakerPersonalia(fødselsdato: LocalDate, programOppstartdato: LocalDate?) = DeltakerPersonalia(
        id = UUID.randomUUID(),
        deltakerIdent = FødselsnummerGenerator.neste(),
        navn = Navn("Ola", null, "Nordmann"),
        fødselsdato = fødselsdato,
        programOppstartdato = programOppstartdato,
        diskresjonskoder = emptySet()
    )

    @Test
    fun `uten programOppstartdato skal førsteMuligeInnmeldingsdato være 18 år + 1 måned etter fødselsdato`() {
        val fødselsdato = LocalDate.of(2000, 1, 15)
        assertEquals(
            LocalDate.of(2018, 2, 15),
            lagDeltakerPersonalia(fødselsdato, null).førsteMuligeInnmeldingsdato
        )
    }

    @Test
    fun `med programOppstartdato senere enn aldersdato skal førsteMuligeInnmeldingsdato override`() {
        val fødselsdato = LocalDate.of(2000, 1, 15)
        val programOppstartdato = LocalDate.of(2025, 8, 1)
        assertEquals(
            LocalDate.of(2025, 8, 1),
            lagDeltakerPersonalia(fødselsdato, programOppstartdato).førsteMuligeInnmeldingsdato
        )
    }

    @Test
    fun `med programOppstartdato tidligere enn aldersdato skal aldersdato vinne for førsteMuligeInnmelding`() {
        val fødselsdato = LocalDate.of(2000, 1, 15)
        val programOppstartdato = LocalDate.of(2017, 1, 1)
        assertEquals(
            LocalDate.of(2018, 2, 15),
            lagDeltakerPersonalia(fødselsdato, programOppstartdato).førsteMuligeInnmeldingsdato
        )
    }

    @Test
    fun `uten programOppstartdato skal sisteMuligeInnmeldingsdato være fødselsdato + 29 år - 1 dag`() {
        val fødselsdato = LocalDate.of(1996, 5, 10)
        assertEquals(
            LocalDate.of(2025, 5, 9),
            lagDeltakerPersonalia(fødselsdato, null).sisteMuligeInnmeldingsdato
        )
    }

    @Test
    fun `med programOppstartdato senere enn sisteMuligeInnmeldingsdato skal siste fødselsdato være siste mulige dato`() {
        val fødselsdato = LocalDate.of(1996, 5, 10)
        val programOppstartdato = LocalDate.of(2025, 8, 1)
        val deltakerPersonalia = lagDeltakerPersonalia(fødselsdato, programOppstartdato)
        assertEquals(
            fødselsdato.plusYears(29).minusDays(1),
            deltakerPersonalia.sisteMuligeInnmeldingsdato
        )

        assertTrue { deltakerPersonalia.førsteMuligeInnmeldingsdato.isAfter(deltakerPersonalia.sisteMuligeInnmeldingsdato) }
    }


    @Test
    fun `med programOppstartdato lik 29 årsdagen`() {
        val fødselsdato = LocalDate.of(1996, 8, 1)
        val programOppstartdato = LocalDate.of(2025, 8, 1)
        val deltakerPersonalia = lagDeltakerPersonalia(fødselsdato, programOppstartdato)
        assertEquals(
            fødselsdato.plusYears(29).minusDays(1),
            deltakerPersonalia.sisteMuligeInnmeldingsdato
        )

        assertTrue { deltakerPersonalia.førsteMuligeInnmeldingsdato.isAfter(deltakerPersonalia.sisteMuligeInnmeldingsdato) }
    }

    @Test
    fun `med programOppstartdato tidligere enn sisteMuligeInnmeldingsdato skal alder vinne`() {
        val fødselsdato = LocalDate.of(1996, 2, 11)
        val programOppstartdato = LocalDate.of(2025, 1, 1)
        assertEquals(
            LocalDate.of(2025, 2, 10),
            lagDeltakerPersonalia(fødselsdato, programOppstartdato).sisteMuligeInnmeldingsdato
        )
    }
}
