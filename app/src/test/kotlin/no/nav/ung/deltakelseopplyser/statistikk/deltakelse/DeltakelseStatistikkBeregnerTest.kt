package no.nav.ung.deltakelseopplyser.statistikk.deltakelse

import no.nav.ung.deltakelseopplyser.historikk.AuditorAwareImpl.Companion.VEILEDER_SUFFIX
import no.nav.ung.deltakelseopplyser.integration.nom.api.domene.OrgEnhetMedPeriode
import no.nav.ung.deltakelseopplyser.integration.nom.api.domene.RessursMedAlleTilknytninger
import no.nav.ung.deltakelseopplyser.integration.nom.api.domene.RessursOrgTilknytningMedPeriode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class DeltakelseStatistikkBeregnerTest {

    private companion object {
        private val beregner = DeltakelsePerEnhetStatistikkTeller()
    }

    @Test
    fun `skal velge mest populære enhet når veileder har flere enheter`() {
        // Gitt følgende deltakelser og ressursdata
        val deltakelser = listOf(
            DeltakelseInput(UUID.randomUUID(), "ABC123$VEILEDER_SUFFIX", LocalDate.parse("2024-01-15")),
            DeltakelseInput(UUID.randomUUID(), "DEF456$VEILEDER_SUFFIX", LocalDate.parse("2024-01-16")),
            DeltakelseInput(
                UUID.randomUUID(),
                "GHI789$VEILEDER_SUFFIX",
                LocalDate.parse("2024-01-17")
            ) // Denne har flere enheter
        )

        val ressurserMedTilknytninger = listOf(
            RessursMedAlleTilknytninger(
                navIdent = "ABC123",
                orgTilknytninger = listOf(
                    RessursOrgTilknytningMedPeriode(
                        gyldigFom = LocalDate.parse("2024-01-01"),
                        gyldigTom = null,
                        orgEnhet = OrgEnhetMedPeriode(
                            id = "1001",
                            navn = "NAV Oslo",
                            gyldigFom = LocalDate.parse("2020-01-01"),
                            gyldigTom = null
                        )
                    )
                )
            ),
            RessursMedAlleTilknytninger(
                navIdent = "DEF456",
                orgTilknytninger = listOf(
                    RessursOrgTilknytningMedPeriode(
                        gyldigFom = LocalDate.parse("2024-01-01"),
                        gyldigTom = null,
                        orgEnhet = OrgEnhetMedPeriode(
                            id = "1001",
                            navn = "NAV Oslo",
                            gyldigFom = LocalDate.parse("2020-01-01"),
                            gyldigTom = null
                        )
                    )
                )
            ),
            RessursMedAlleTilknytninger(
                navIdent = "GHI789",
                orgTilknytninger = listOf(
                    RessursOrgTilknytningMedPeriode(
                        gyldigFom = LocalDate.parse("2024-01-01"),
                        gyldigTom = null,
                        orgEnhet = OrgEnhetMedPeriode(
                            id = "1001",
                            navn = "NAV Oslo",
                            gyldigFom = LocalDate.parse("2020-01-01"),
                            gyldigTom = null
                        )
                    ),
                    RessursOrgTilknytningMedPeriode(
                        gyldigFom = LocalDate.parse("2024-01-01"),
                        gyldigTom = null,
                        orgEnhet = OrgEnhetMedPeriode(
                            id = "9999",
                            navn = "NAV Regionkontor",
                            gyldigFom = LocalDate.parse("2020-01-01"),
                            gyldigTom = null
                        )
                    )
                )
            )
        )

        // Når vi beregner antall deltakelser per enhet
        val resultat = beregner.tellAntallDeltakelserPerEnhet(deltakelser, ressurserMedTilknytninger)

        // Da skal NAV Oslo velges fordi den er mest populær (3 forekomster totalt vs 1 for Regionkontor)
        assertThat(resultat.deltakelserPerEnhet).containsEntry("NAV Oslo", 3)
        assertThat(resultat.deltakelserPerEnhet).doesNotContainKey("NAV Regionkontor")
    }

    @Test
    fun `skal håndtere veiledere uten enheter`() {
        // Gitt deltakelser hvor en veileder ikke finnes i ressursdata
        val deltakelser = listOf(
            DeltakelseInput(UUID.randomUUID(), "ABC123$VEILEDER_SUFFIX", LocalDate.parse("2024-01-15")),
            DeltakelseInput(UUID.randomUUID(), "UNKNOWN$VEILEDER_SUFFIX", LocalDate.parse("2024-01-16"))
        )

        val ressurserMedTilknytninger = listOf(
            RessursMedAlleTilknytninger(
                navIdent = "ABC123",
                orgTilknytninger = listOf(
                    RessursOrgTilknytningMedPeriode(
                        gyldigFom = LocalDate.parse("2024-01-01"),
                        gyldigTom = null,
                        orgEnhet = OrgEnhetMedPeriode(
                            id = "1001",
                            navn = "NAV Oslo",
                            gyldigFom = LocalDate.parse("2020-01-01"),
                            gyldigTom = null
                        )
                    )
                )
            )
            // UNKNOWN mangler i ressursene
        )

        // Når vi beregner antall deltakelser per enhet
        val resultat = beregner.tellAntallDeltakelserPerEnhet(deltakelser, ressurserMedTilknytninger)

        // Da skal kun deltakelsen fra ABC123 telles
        assertThat(resultat.deltakelserPerEnhet).containsEntry("NAV Oslo", 1)
    }

    @Test
    fun `skal velge første enhet hvis flere enheter har samme popularitet`() {
        // Gitt en deltakelse hvor veilederen har to enheter med lik popularitet
        val deltakelser = listOf(
            DeltakelseInput(UUID.randomUUID(), "ABC123$VEILEDER_SUFFIX", LocalDate.parse("2024-01-15"))
        )

        val ressurserMedTilknytninger = listOf(
            RessursMedAlleTilknytninger(
                navIdent = "ABC123",
                orgTilknytninger = listOf(
                    RessursOrgTilknytningMedPeriode(
                        gyldigFom = LocalDate.parse("2024-01-01"),
                        gyldigTom = null,
                        orgEnhet = OrgEnhetMedPeriode(
                            id = "1001",
                            navn = "NAV Oslo",
                            gyldigFom = LocalDate.parse("2020-01-01"),
                            gyldigTom = null
                        )
                    ),
                    RessursOrgTilknytningMedPeriode(
                        gyldigFom = LocalDate.parse("2024-01-01"),
                        gyldigTom = null,
                        orgEnhet = OrgEnhetMedPeriode(
                            id = "1002",
                            navn = "NAV Bergen",
                            gyldigFom = LocalDate.parse("2020-01-01"),
                            gyldigTom = null
                        )
                    )
                    // Begge har 0 forekomster andre steder, så første skal velges
                )
            )
        )

        // Når vi beregner antall deltakelser per enhet
        val resultat = beregner.tellAntallDeltakelserPerEnhet(deltakelser, ressurserMedTilknytninger)

        // Da skal første enhet (NAV Oslo) velges
        assertThat(resultat.deltakelserPerEnhet).containsEntry("NAV Oslo", 1)
        assertThat(resultat.deltakelserPerEnhet).doesNotContainKey("NAV Bergen")
    }

    @Test
    fun `skal filtrere enheter basert på gyldighetstidspunkt når bruker har flere enheter med perioder`() {
        // Gitt deltakelser på forskjellige datoer
        val deltakelser = listOf(
            DeltakelseInput(UUID.randomUUID(), "ABC123$VEILEDER_SUFFIX", LocalDate.parse("2024-01-15")),
            DeltakelseInput(UUID.randomUUID(), "ABC123$VEILEDER_SUFFIX", LocalDate.parse("2024-06-15"))
        )

        val ressurserMedTilknytninger = listOf(
            RessursMedAlleTilknytninger(
                navIdent = "ABC123",
                orgTilknytninger = listOf(
                    // Tilknytning til NAV Oslo gyldig hele 2024
                    RessursOrgTilknytningMedPeriode(
                        gyldigFom = LocalDate.parse("2024-01-01"),
                        gyldigTom = LocalDate.parse("2024-12-31"),
                        orgEnhet = OrgEnhetMedPeriode(
                            id = "1001",
                            navn = "NAV Oslo",
                            gyldigFom = LocalDate.parse("2020-01-01"),
                            gyldigTom = null
                        )
                    ),
                    // Tilknytning til NAV Bergen kun gyldig første halvdel av 2024
                    RessursOrgTilknytningMedPeriode(
                        gyldigFom = LocalDate.parse("2024-01-01"),
                        gyldigTom = LocalDate.parse("2024-03-31"),
                        orgEnhet = OrgEnhetMedPeriode(
                            id = "1002",
                            navn = "NAV Bergen",
                            gyldigFom = LocalDate.parse("2020-01-01"),
                            gyldigTom = null
                        )
                    )
                )
            )
        )

        // Når vi beregner deltakelser med periode-filtrering
        val resultat = beregner.tellAntallDeltakelserPerEnhet(deltakelser, ressurserMedTilknytninger)

        // Da skal:
        // For deltakelse 15. januar: Både NAV Oslo og NAV Bergen er gyldige, NAV Oslo velges (mest populær)
        // For deltakelse 15. juni: Kun NAV Oslo er gyldig (Bergen-tilknytning utløpt)
        // Resultat: NAV Oslo får begge deltakelsene (2 stk)
        assertThat(resultat.deltakelserPerEnhet).containsEntry("NAV Oslo", 2)
        assertThat(resultat.deltakelserPerEnhet).doesNotContainKey("NAV Bergen")
    }

    @Test
    fun `skal håndtere enheter som ikke er gyldige på deltakelsestidspunkt`() {
        // Gitt en deltakelse hvor ingen enheter er gyldige på deltakelsesdatoen
        val deltakelser = listOf(
            DeltakelseInput(UUID.randomUUID(), "ABC123$VEILEDER_SUFFIX", LocalDate.parse("2024-06-15"))
        )

        val ressurserMedTilknytninger = listOf(
            RessursMedAlleTilknytninger(
                navIdent = "ABC123",
                orgTilknytninger = listOf(
                    // Tilknytning som var gyldig før deltakelsesdato
                    RessursOrgTilknytningMedPeriode(
                        gyldigFom = LocalDate.parse("2024-01-01"),
                        gyldigTom = LocalDate.parse("2024-03-31"),
                        orgEnhet = OrgEnhetMedPeriode(
                            id = "1001",
                            navn = "NAV Oslo",
                            gyldigFom = LocalDate.parse("2020-01-01"),
                            gyldigTom = null
                        )
                    ),
                    // Enhet som ikke var gyldig på deltakelsesdato
                    RessursOrgTilknytningMedPeriode(
                        gyldigFom = LocalDate.parse("2024-01-01"),
                        gyldigTom = null,
                        orgEnhet = OrgEnhetMedPeriode(
                            id = "1002",
                            navn = "NAV Bergen",
                            gyldigFom = LocalDate.parse("2024-07-01"), // Starter etter deltakelsesdato
                            gyldigTom = null
                        )
                    )
                )
            )
        )

        // Når vi beregner antall deltakelser per enhet
        val resultat = beregner.tellAntallDeltakelserPerEnhet(deltakelser, ressurserMedTilknytninger)

        // Da skal ingen deltakelser telles (ingen enheter var gyldige på tidspunktet)
        assertThat(resultat.deltakelserPerEnhet).isEmpty()
    }

    @Test
    fun `skal velge riktig enhet når flere tilknytninger er gyldige på samme tidspunkt`() {
        // Gitt deltakelser hvor flere tilknytninger er gyldige samtidig
        val deltakelser = listOf(
            DeltakelseInput(UUID.randomUUID(), "ABC123$VEILEDER_SUFFIX", LocalDate.parse("2024-02-15")),
            DeltakelseInput(UUID.randomUUID(), "DEF456$VEILEDER_SUFFIX", LocalDate.parse("2024-02-15"))
        )

        val ressurserMedTilknytninger = listOf(
            RessursMedAlleTilknytninger(
                navIdent = "ABC123",
                orgTilknytninger = listOf(
                    RessursOrgTilknytningMedPeriode(
                        gyldigFom = LocalDate.parse("2024-01-01"),
                        gyldigTom = null,
                        orgEnhet = OrgEnhetMedPeriode(
                            id = "1001",
                            navn = "NAV Oslo",
                            gyldigFom = LocalDate.parse("2020-01-01"),
                            gyldigTom = null
                        )
                    ),
                    RessursOrgTilknytningMedPeriode(
                        gyldigFom = LocalDate.parse("2024-02-01"),
                        gyldigTom = LocalDate.parse("2024-12-31"),
                        orgEnhet = OrgEnhetMedPeriode(
                            id = "1003",
                            navn = "NAV Trondheim",
                            gyldigFom = LocalDate.parse("2020-01-01"),
                            gyldigTom = null
                        )
                    )
                )
            ),
            RessursMedAlleTilknytninger(
                navIdent = "DEF456",
                orgTilknytninger = listOf(
                    RessursOrgTilknytningMedPeriode(
                        gyldigFom = LocalDate.parse("2024-01-01"),
                        gyldigTom = null,
                        orgEnhet = OrgEnhetMedPeriode(
                            id = "1001",
                            navn = "NAV Oslo",
                            gyldigFom = LocalDate.parse("2020-01-01"),
                            gyldigTom = null
                        )
                    )
                )
            )
        )

        // Når vi beregner antall deltakelser per enhet
        val resultat = beregner.tellAntallDeltakelserPerEnhet(deltakelser, ressurserMedTilknytninger)

        // Da skal:
        // ABC123 har både NAV Oslo og NAV Trondheim gyldige på 15. februar
        // NAV Oslo skal velges fordi den er mest populær (2 forekomster vs 1)
        assertThat(resultat.deltakelserPerEnhet).containsEntry("NAV Oslo", 2)
        assertThat(resultat.deltakelserPerEnhet).doesNotContainKey("NAV Trondheim")
    }

    @Test
    fun `skal håndtere overlappende perioder korrekt`() {
        // Gitt deltakelser som faller i forskjellige gyldighetsperioder for samme veileder
        val deltakelser = listOf(
            DeltakelseInput(UUID.randomUUID(), "ABC123$VEILEDER_SUFFIX", LocalDate.parse("2024-01-15")),
            DeltakelseInput(UUID.randomUUID(), "ABC123$VEILEDER_SUFFIX", LocalDate.parse("2024-03-15"))
        )

        val ressurserMedTilknytninger = listOf(
            RessursMedAlleTilknytninger(
                navIdent = "ABC123",
                orgTilknytninger = listOf(
                    // Første tilknytning gyldig i januar-februar
                    RessursOrgTilknytningMedPeriode(
                        gyldigFom = LocalDate.parse("2024-01-01"),
                        gyldigTom = LocalDate.parse("2024-02-29"),
                        orgEnhet = OrgEnhetMedPeriode(
                            id = "1001",
                            navn = "NAV Oslo",
                            gyldigFom = LocalDate.parse("2020-01-01"),
                            gyldigTom = null
                        )
                    ),
                    // Andre tilknytning gyldig fra mars
                    RessursOrgTilknytningMedPeriode(
                        gyldigFom = LocalDate.parse("2024-03-01"),
                        gyldigTom = null,
                        orgEnhet = OrgEnhetMedPeriode(
                            id = "1002",
                            navn = "NAV Bergen",
                            gyldigFom = LocalDate.parse("2020-01-01"),
                            gyldigTom = null
                        )
                    )
                )
            )
        )

        // Når vi beregner antall deltakelser per enhet
        val resultat = beregner.tellAntallDeltakelserPerEnhet(deltakelser, ressurserMedTilknytninger)

        // Da skal hver deltakelse mappes til riktig enhet basert på tidspunkt:
        // Første deltakelse (15. januar) -> NAV Oslo
        // Andre deltakelse (15. mars) -> NAV Bergen
        assertThat(resultat.deltakelserPerEnhet).containsEntry("NAV Oslo", 1)
        assertThat(resultat.deltakelserPerEnhet).containsEntry("NAV Bergen", 1)
    }
}
