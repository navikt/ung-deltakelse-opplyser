package no.nav.ung.deltakelseopplyser.domene.deltaker

import no.nav.pdl.generated.hentperson.Navn
import no.nav.ung.deltakelseopplyser.utils.FødselsnummerGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.test.assertTrue

internal class DeltakerPersonaliaTest {

    private fun lagDeltakerPersonalia(fødselsdato: LocalDate, programOppstartdato: LocalDate) = DeltakerPersonalia(
        id = UUID.randomUUID(),
        deltakerIdent = FødselsnummerGenerator.neste(),
        navn = Navn("Ola", null, "Nordmann"),
        fødselsdato = fødselsdato,
        programOppstartdato = programOppstartdato,
        diskresjonskoder = emptySet()
    )

    @Test
    fun `Dersom 18-årsdagen er før programOppstartdato skal programOppstartdato velges for førsteMuligeInnmeldingsdato`() {
        val fødselsdato = LocalDate.of(2000, 1, 15)
        val programOppstartdato = LocalDate.of(2025, 8, 1)
        assertEquals(
            LocalDate.of(2025, 8, 1),
            lagDeltakerPersonalia(fødselsdato, programOppstartdato).førsteMuligeInnmeldingsdato
        )
    }

    @Test
    fun `Dersom 18-årsdagen er etter programOppstartdato skal 18-årsdagen velges for førsteMuligeInnmeldingsdato`() {
        val fødselsdato = LocalDate.of(2000, 1, 15)
        val programOppstartdato = LocalDate.of(2017, 1, 1)
        assertEquals(
            LocalDate.of(2018, 1, 15),
            lagDeltakerPersonalia(fødselsdato, programOppstartdato).førsteMuligeInnmeldingsdato
        )
    }

    @Test
    fun `Dersom 18-årsdagen er lik programOppstartdato skal 18-årsdagen velges for førsteMuligeInnmeldingsdato`() {
        val fødselsdato = LocalDate.of(2000, 1, 15)
        val programOppstartdato = LocalDate.of(2018, 1, 15) // Samme dato som 18-årsdagen
        assertEquals(
            LocalDate.of(2018, 1, 15),
            lagDeltakerPersonalia(fødselsdato, programOppstartdato).førsteMuligeInnmeldingsdato
        )
    }

    @Test
    fun `sisteMuligeInnmeldingsdato skal være lik dagen før 29 års dagen`() {
        val fødselsdato = LocalDate.of(1996, 8, 1)
        val programOppstartdato = LocalDate.of(2025, 8, 1)
        val deltakerPersonalia = lagDeltakerPersonalia(fødselsdato, programOppstartdato)
        assertEquals(
            fødselsdato.plusYears(29).minusDays(1),
            deltakerPersonalia.sisteMuligeInnmeldingsdato
        )

        assertTrue { deltakerPersonalia.førsteMuligeInnmeldingsdato.isAfter(deltakerPersonalia.sisteMuligeInnmeldingsdato) }
    }
}
