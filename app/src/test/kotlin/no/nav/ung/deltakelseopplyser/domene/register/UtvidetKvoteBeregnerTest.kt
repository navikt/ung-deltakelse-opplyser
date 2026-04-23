package no.nav.ung.deltakelseopplyser.domene.register

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate

class UtvidetKvoteBeregnerTest {

    @Test
    fun `finnSluttdatoForVirkedager - 5 virkedager fra mandag gir fredag samme uke`() {
        val mandag = LocalDate.of(2025, 1, 6) // mandag
        val resultat = UtvidetKvoteBeregner.finnSluttdatoForVirkedager(mandag, 5)
        assertThat(resultat).isEqualTo(LocalDate.of(2025, 1, 10)) // fredag
    }

    @Test
    fun `finnSluttdatoForVirkedager - 6 virkedager fra mandag gir mandag neste uke`() {
        val mandag = LocalDate.of(2025, 1, 6)
        val resultat = UtvidetKvoteBeregner.finnSluttdatoForVirkedager(mandag, 6)
        assertThat(resultat).isEqualTo(LocalDate.of(2025, 1, 13)) // mandag neste uke
    }

    @Test
    fun `finnSluttdatoForVirkedager - 10 virkedager fra mandag gir fredag neste uke`() {
        val mandag = LocalDate.of(2025, 1, 6)
        val resultat = UtvidetKvoteBeregner.finnSluttdatoForVirkedager(mandag, 10)
        assertThat(resultat).isEqualTo(LocalDate.of(2025, 1, 17)) // fredag neste uke
    }

    @Test
    fun `finnSluttdatoForVirkedager - 1 virkedag fra fredag gir fredag`() {
        val fredag = LocalDate.of(2025, 1, 10)
        val resultat = UtvidetKvoteBeregner.finnSluttdatoForVirkedager(fredag, 1)
        assertThat(resultat).isEqualTo(fredag)
    }

    @Test
    fun `finnSluttdatoForVirkedager - 2 virkedager fra fredag gir mandag`() {
        val fredag = LocalDate.of(2025, 1, 10)
        val resultat = UtvidetKvoteBeregner.finnSluttdatoForVirkedager(fredag, 2)
        assertThat(resultat).isEqualTo(LocalDate.of(2025, 1, 13)) // mandag
    }

    @Test
    fun `finnSluttdatoForVirkedager - 300 virkedager teller ikke helger`() {
        val startdato = LocalDate.of(2025, 1, 6) // mandag
        val resultat = UtvidetKvoteBeregner.finnSluttdatoForVirkedager(startdato, 300)

        // Verifiser at sluttdatoen er en virkedag
        assertThat(resultat.dayOfWeek).isNotIn(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

        // Tell virkedager manuelt for å verifisere
        var antallVirkedager = 0L
        var dato = startdato
        while (dato <= resultat) {
            if (dato.dayOfWeek != DayOfWeek.SATURDAY && dato.dayOfWeek != DayOfWeek.SUNDAY) {
                antallVirkedager++
            }
            dato = dato.plusDays(1)
        }
        assertThat(antallVirkedager).isEqualTo(300)
    }

    @Test
    fun `finnSluttdatoForVirkedager - startdato paa onsdag`() {
        val onsdag = LocalDate.of(2025, 1, 8)
        val resultat = UtvidetKvoteBeregner.finnSluttdatoForVirkedager(onsdag, 5)
        // ons, tor, fre, man, tir = 5 virkedager
        assertThat(resultat).isEqualTo(LocalDate.of(2025, 1, 14)) // tirsdag
    }
}

