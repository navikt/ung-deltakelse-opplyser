package no.nav.ung.deltakelseopplyser.statistikk.deltakelse

import no.nav.ung.deltakelseopplyser.historikk.AuditorAwareImpl.Companion.VEILEDER_SUFFIX
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

class DeltakelseStatistikkBeregnerTest {

    private companion object {
        private val beregner = DeltakelseStatistikkBeregner()
    }

    @Test
    fun `skal velge mest populære enhet når veileder har flere enheter`() {
        // Gitt følgende deltakelser og ressursdata
        val deltakelser = listOf(
            DeltakelseInput(UUID.randomUUID(), "ABC123$VEILEDER_SUFFIX", LocalDate.of(2024, 1, 15)),
            DeltakelseInput(UUID.randomUUID(), "DEF456$VEILEDER_SUFFIX", LocalDate.of(2024, 1, 16)),
            DeltakelseInput(UUID.randomUUID(), "GHI789$VEILEDER_SUFFIX", LocalDate.of(2024, 1, 17)) // Denne har flere enheter
        )

        val ressurserMedEnheter = listOf(
            RessursMedEnheterInput(
                navIdent = "ABC123",
                enheter = listOf(OrgEnhetInput("1001", "NAV Oslo"))
            ),
            RessursMedEnheterInput(
                navIdent = "DEF456",
                enheter = listOf(OrgEnhetInput("1001", "NAV Oslo"))
            ),
            RessursMedEnheterInput(
                navIdent = "GHI789",
                enheter = listOf(
                    OrgEnhetInput("1001", "NAV Oslo"),        // Populær (3 forekomster totalt)
                    OrgEnhetInput("9999", "NAV Regionkontor") // Mindre populær (1 forekomst)
                )
            )
        )

        // beregn antall deltakelser per enhet
        val resultat = beregner.beregnAntallDeltakelserPerEnhet(deltakelser, ressurserMedEnheter)

        assertThat(resultat.deltakelserPerEnhet).containsEntry("NAV Oslo", 3)
        assertThat(resultat.deltakelserPerEnhet).doesNotContainKey("NAV Regionkontor")
    }

    @Test
    fun `skal håndtere veiledere uten enheter`() {
        // Gitt følgende deltakelser og ressursdata
        val deltakelser = listOf(
            DeltakelseInput(UUID.randomUUID(), "ABC123$VEILEDER_SUFFIX", LocalDate.of(2024, 1, 15)),
            DeltakelseInput(UUID.randomUUID(), "UNKNOWN$VEILEDER_SUFFIX", LocalDate.of(2024, 1, 16))
        )

        val ressurserMedEnheter = listOf(
            RessursMedEnheterInput(
                navIdent = "ABC123",
                enheter = listOf(OrgEnhetInput("1001", "NAV Oslo"))
            )
            // UNKNOWN mangler i ressursene
        )

        // beregn antall deltakelser per enhet
        val resultat = beregner.beregnAntallDeltakelserPerEnhet(deltakelser, ressurserMedEnheter)

        assertThat(resultat.deltakelserPerEnhet).containsEntry("NAV Oslo", 1)
    }

    @Test
    fun `skal konvertere til statistikk records korrekt`() {
        // Gitt følgende deltakelser og ressursdata
        val deltakelsePerEnhetResultat = DeltakelsePerEnhetResultat(
            deltakelserPerEnhet = mapOf("NAV Oslo" to 5, "NAV Bergen" to 3)
        )
        val kjøringstidspunkt = ZonedDateTime.now()

        // beregn antall deltakelser per enhet
        val records = beregner.konverterTilStatistikkRecords(deltakelsePerEnhetResultat, kjøringstidspunkt)

        assertThat(records).hasSize(2)
        assertThat(records.map { it.kontor }).containsExactlyInAnyOrder("NAV Oslo", "NAV Bergen")
        assertThat(records.find { it.kontor == "NAV Oslo" }?.antallDeltakelser).isEqualTo(5)
        assertThat(records.find { it.kontor == "NAV Bergen" }?.antallDeltakelser).isEqualTo(3)
    }

    @Test
    fun `skal velge første enhet hvis flere enheter har samme popularitet`() {
        // Gitt følgende deltakelser og ressursdata
        val deltakelser = listOf(
            DeltakelseInput(UUID.randomUUID(), "ABC123$VEILEDER_SUFFIX", LocalDate.of(2024, 1, 15))
        )

        val ressurserMedEnheter = listOf(
            RessursMedEnheterInput(
                navIdent = "ABC123",
                enheter = listOf(
                    OrgEnhetInput("1001", "NAV Oslo"),
                    OrgEnhetInput("1002", "NAV Bergen")
                    // Begge har 0 forekomster andre steder, så første skal velges
                )
            )
        )

        // beregn antall deltakelser per enhet
        val resultat = beregner.beregnAntallDeltakelserPerEnhet(deltakelser, ressurserMedEnheter)

        assertThat(resultat.deltakelserPerEnhet).containsEntry("NAV Oslo", 1)
        assertThat(resultat.deltakelserPerEnhet).doesNotContainKey("NAV Bergen")
    }
}
