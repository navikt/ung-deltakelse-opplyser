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

        // Da skal deltakelsen fra ABC123 telles under NAV Oslo,
        // og deltakelsen fra UNKNOWN telles under "Enhet sikkerhetsnett"
        assertThat(resultat.deltakelserPerEnhet).containsEntry("NAV Oslo", 1)
        assertThat(resultat.deltakelserPerEnhet).containsEntry(DeltakelsePerEnhetStatistikkTeller.ENHET_SIKKERHETSNETT, 1)
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
    fun `skal bruke nærmeste tilknytning når enheter ikke er gyldige på deltakelsestidspunkt`() {
        // Gitt en deltakelse hvor ingen enheter er gyldige på deltakelsesdatoen.
        // Nærmeste-tilknytning-fallback skal finne nærmeste tilknytning.
        val deltakelser = listOf(
            DeltakelseInput(UUID.randomUUID(), "ABC123$VEILEDER_SUFFIX", LocalDate.parse("2024-08-15"))
        )

        val ressurserMedTilknytninger = listOf(
            RessursMedAlleTilknytninger(
                navIdent = "ABC123",
                orgTilknytninger = listOf(
                    // Tilknytning som utløp mer enn 90 dager før deltakelsesdato
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
                    // Tilknytning som er gyldig men orgEnheten starter etter deltakelsesdato
                    RessursOrgTilknytningMedPeriode(
                        gyldigFom = LocalDate.parse("2024-01-01"),
                        gyldigTom = null,
                        orgEnhet = OrgEnhetMedPeriode(
                            id = "1002",
                            navn = "NAV Bergen",
                            gyldigFom = LocalDate.parse("2024-09-01"),
                            gyldigTom = null
                        )
                    )
                )
            )
        )

        val resultat = beregner.tellAntallDeltakelserPerEnhet(deltakelser, ressurserMedTilknytninger)

        // Nærmeste-tilknytning-fallback velger tilknytning 2 (NAV Bergen) som har avstand 0
        // (deltakelsesdatoen er innenfor tilknytningsperioden), noe som er bedre enn sikkerhetsnett.
        assertThat(resultat.deltakelserPerEnhet).containsEntry("NAV Bergen", 1)
        assertThat(resultat.deltakelserPerEnhet).doesNotContainKey(DeltakelsePerEnhetStatistikkTeller.ENHET_SIKKERHETSNETT)
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

    @Test
    fun `skal bruke siste gyldige enhet når deltakelse opprettes i gap-periode mellom enhetsbytte`() {
        // Gitt en deltakelse opprettet i en gap-periode mellom to tilknytninger
        val deltakelser = listOf(
            DeltakelseInput(UUID.randomUUID(), "ABC123$VEILEDER_SUFFIX", LocalDate.parse("2025-10-09"))
        )

        val ressurserMedTilknytninger = listOf(
            RessursMedAlleTilknytninger(
                navIdent = "ABC123",
                orgTilknytninger = listOf(
                    // Gammel tilknytning som utløp 30. september
                    RessursOrgTilknytningMedPeriode(
                        gyldigFom = LocalDate.parse("2024-01-01"),
                        gyldigTom = LocalDate.parse("2025-09-30"),
                        orgEnhet = OrgEnhetMedPeriode(
                            id = "1001",
                            navn = "NAV Oslo",
                            gyldigFom = LocalDate.parse("2020-01-01"),
                            gyldigTom = "2025-09-30".let { LocalDate.parse(it) }
                        )
                    ),
                    // Ny tilknytning som starter 20. oktober
                    RessursOrgTilknytningMedPeriode(
                        gyldigFom = LocalDate.parse("2025-10-20"),
                        gyldigTom = null,
                        orgEnhet = OrgEnhetMedPeriode(
                            id = "1002",
                            navn = "NAV Bergen",
                            gyldigFom = LocalDate.parse("2025-10-01"),
                            gyldigTom = null
                        )
                    )
                )
            )
        )

        // Når vi beregner antall deltakelser per enhet
        val resultat = beregner.tellAntallDeltakelserPerEnhet(deltakelser, ressurserMedTilknytninger)

        // Da skal deltakelsen tildeles den gamle enheten (NAV Oslo)
        // fordi den var siste gyldige enhet veilederen hadde
        assertThat(resultat.deltakelserPerEnhet).containsEntry("NAV Oslo", 1)
        assertThat(resultat.deltakelserPerEnhet).doesNotContainKey("NAV Bergen")
    }

    @Test
    fun `skal bruke nærmeste tilknytning når NOM-registrering kommer etter deltakelse-opprettelse`() {
        // Gitt en deltakelse opprettet FØR veilederens NOM-tilknytning starter
        val deltakelser = listOf(
            DeltakelseInput(UUID.randomUUID(), "ABC123$VEILEDER_SUFFIX", LocalDate.parse("2025-10-09"))
        )

        val ressurserMedTilknytninger = listOf(
            RessursMedAlleTilknytninger(
                navIdent = "ABC123",
                orgTilknytninger = listOf(
                    // Tilknytning som starter 11 dager etter deltakelsesdato
                    RessursOrgTilknytningMedPeriode(
                        gyldigFom = LocalDate.parse("2025-10-20"),
                        gyldigTom = null,
                        orgEnhet = OrgEnhetMedPeriode(
                            id = "1001",
                            navn = "Skien Oppfølging ung",
                            gyldigFom = LocalDate.parse("2025-10-01"),
                            gyldigTom = null
                        )
                    )
                )
            )
        )

        // Når vi beregner antall deltakelser per enhet
        val resultat = beregner.tellAntallDeltakelserPerEnhet(deltakelser, ressurserMedTilknytninger)

        // Da skal deltakelsen mappes via nærmeste-tilknytning-fallback til den kommende enheten
        assertThat(resultat.deltakelserPerEnhet).containsEntry("Skien Oppfølging ung", 1)
        assertThat(resultat.deltakelserPerEnhet).doesNotContainKey(DeltakelsePerEnhetStatistikkTeller.ENHET_SIKKERHETSNETT)
    }

    @Test
    fun `skal bruke nærmeste tilknytning når gap er større enn 90 dager fremover`() {
        // Gitt en deltakelse opprettet mer enn 90 dager FØR veilederens NOM-tilknytning starter.
        // Nærmeste-tilknytning-fallback fanger denne.
        val deltakelser = listOf(
            DeltakelseInput(UUID.randomUUID(), "ABC123$VEILEDER_SUFFIX", LocalDate.parse("2025-09-11"))
        )

        val ressurserMedTilknytninger = listOf(
            RessursMedAlleTilknytninger(
                navIdent = "ABC123",
                orgTilknytninger = listOf(
                    // Tilknytning som starter 112 dager etter deltakelsesdato (> 90 dager)
                    RessursOrgTilknytningMedPeriode(
                        gyldigFom = LocalDate.parse("2026-01-01"),
                        gyldigTom = null,
                        orgEnhet = OrgEnhetMedPeriode(
                            id = "1001",
                            navn = "Kristiansand Ungdom 1",
                            gyldigFom = LocalDate.parse("2020-01-01"),
                            gyldigTom = null
                        )
                    )
                )
            )
        )

        val resultat = beregner.tellAntallDeltakelserPerEnhet(deltakelser, ressurserMedTilknytninger)

        // Nærmeste-tilknytning-fallback fanger denne — nærmeste tilknytning uavhengig av avstand
        assertThat(resultat.deltakelserPerEnhet).containsEntry("Kristiansand Ungdom 1", 1)
        assertThat(resultat.deltakelserPerEnhet).doesNotContainKey(DeltakelsePerEnhetStatistikkTeller.ENHET_SIKKERHETSNETT)
    }

    @Test
    fun `summen av alle enheter skal alltid være lik antall input-deltakelser`() {
        // Gitt en blanding av deltakelser med og uten gyldig enhet
        val deltakelser = listOf(
            DeltakelseInput(UUID.randomUUID(), "ABC123$VEILEDER_SUFFIX", LocalDate.parse("2024-01-15")),
            DeltakelseInput(UUID.randomUUID(), "UNKNOWN$VEILEDER_SUFFIX", LocalDate.parse("2024-01-16")),
            DeltakelseInput(UUID.randomUUID(), "DEF456$VEILEDER_SUFFIX", LocalDate.parse("2024-08-15")) // Gap > 90 dager, men nærmeste-tilknytning finner enheten
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
                        gyldigTom = LocalDate.parse("2024-03-31"),
                        orgEnhet = OrgEnhetMedPeriode(
                            id = "1002",
                            navn = "NAV Bergen",
                            gyldigFom = LocalDate.parse("2020-01-01"),
                            gyldigTom = LocalDate.parse("2024-03-31")
                        )
                    )
                )
            )
            // UNKNOWN mangler i ressursene
        )

        // Når vi beregner antall deltakelser per enhet
        val resultat = beregner.tellAntallDeltakelserPerEnhet(deltakelser, ressurserMedTilknytninger)

        // Da skal summen av alle enheter (inkludert "Enhet sikkerhetsnett") alltid være lik antall input-deltakelser
        val totalSum = resultat.deltakelserPerEnhet.values.sum()
        assertThat(totalSum).isEqualTo(deltakelser.size)

        // ABC123 → NAV Oslo, DEF456 → NAV Bergen (via nærmeste tilknytning), UNKNOWN → sikkerhetsnett
        assertThat(resultat.deltakelserPerEnhet).containsEntry("NAV Oslo", 1)
        assertThat(resultat.deltakelserPerEnhet).containsEntry("NAV Bergen", 1)
        assertThat(resultat.deltakelserPerEnhet).containsEntry(DeltakelsePerEnhetStatistikkTeller.ENHET_SIKKERHETSNETT, 1)

        // Diagnostikken viser kun UNKNOWN som umappet (DEF456 løst av nærmeste-tilknytning-fallback)
        @Suppress("UNCHECKED_CAST")
        val deltakelserUtenEnhet = resultat.diagnostikk["deltakelserUtenEnhet"] as Map<String, Any>
        assertThat(deltakelserUtenEnhet["antall"]).isEqualTo(1) // Kun UNKNOWN (ikke i NOM)
    }

    @Test
    fun `nærmeste tilknytning - skal bruke nærmeste tilknytning fremover når gap er stort`() {
        // Gitt en deltakelse opprettet mer enn 90 dager FØR veilederens NOM-tilknytning starter.
        // Nærmeste-tilknytning-fallback skal finne nærmeste tilknytning uavhengig av avstand
        val deltakelser = listOf(
            DeltakelseInput(UUID.randomUUID(), "ABC123$VEILEDER_SUFFIX", LocalDate.parse("2025-06-01"))
        )

        val ressurserMedTilknytninger = listOf(
            RessursMedAlleTilknytninger(
                navIdent = "ABC123",
                orgTilknytninger = listOf(
                    // Tilknytning som starter 150 dager etter deltakelsesdato
                    RessursOrgTilknytningMedPeriode(
                        gyldigFom = LocalDate.parse("2025-10-29"),
                        gyldigTom = null,
                        orgEnhet = OrgEnhetMedPeriode(
                            id = "1001",
                            navn = "Skien Oppfølging ung",
                            gyldigFom = LocalDate.parse("2025-10-01"),
                            gyldigTom = null
                        )
                    )
                )
            )
        )

        val resultat = beregner.tellAntallDeltakelserPerEnhet(deltakelser, ressurserMedTilknytninger)

        // Nærmeste-tilknytning-fallback skal finne nærmeste tilknytning uavhengig av avstand
        assertThat(resultat.deltakelserPerEnhet).containsEntry("Skien Oppfølging ung", 1)
        assertThat(resultat.deltakelserPerEnhet).doesNotContainKey(DeltakelsePerEnhetStatistikkTeller.ENHET_SIKKERHETSNETT)
    }

    @Test
    fun `nærmeste tilknytning - skal bruke nærmeste tilknytning bakover når gap er stort`() {
        // Gitt deltakelse som ligger i en gap-periode bakover i tid
        val deltakelser = listOf(
            DeltakelseInput(UUID.randomUUID(), "ABC123$VEILEDER_SUFFIX", LocalDate.parse("2025-12-01"))
        )

        val ressurserMedTilknytninger = listOf(
            RessursMedAlleTilknytninger(
                navIdent = "ABC123",
                orgTilknytninger = listOf(
                    RessursOrgTilknytningMedPeriode(
                        gyldigFom = LocalDate.parse("2025-01-01"),
                        gyldigTom = LocalDate.parse("2025-06-30"),
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

        val resultat = beregner.tellAntallDeltakelserPerEnhet(deltakelser, ressurserMedTilknytninger)

        assertThat(resultat.deltakelserPerEnhet).containsEntry("NAV Oslo", 1)
        assertThat(resultat.deltakelserPerEnhet).doesNotContainKey(DeltakelsePerEnhetStatistikkTeller.ENHET_SIKKERHETSNETT)
    }

    @Test
    fun `nærmeste tilknytning - skal velge nærmeste av flere tilknytninger`() {
        // Gitt deltakelse hvor det finnes flere tilknytninger utenfor toleranse
        val deltakelser = listOf(
            DeltakelseInput(UUID.randomUUID(), "ABC123$VEILEDER_SUFFIX", LocalDate.parse("2025-07-01"))
        )

        val ressurserMedTilknytninger = listOf(
            RessursMedAlleTilknytninger(
                navIdent = "ABC123",
                orgTilknytninger = listOf(
                    // 182 dager før
                    RessursOrgTilknytningMedPeriode(
                        gyldigFom = LocalDate.parse("2024-06-01"),
                        gyldigTom = LocalDate.parse("2024-12-31"),
                        orgEnhet = OrgEnhetMedPeriode(
                            id = "1001",
                            navn = "NAV Oslo",
                            gyldigFom = LocalDate.parse("2020-01-01"),
                            gyldigTom = null
                        )
                    ),
                    // 123 dager etter — nærmere
                    RessursOrgTilknytningMedPeriode(
                        gyldigFom = LocalDate.parse("2025-11-01"),
                        gyldigTom = null,
                        orgEnhet = OrgEnhetMedPeriode(
                            id = "1002",
                            navn = "Skien Oppfølging ung",
                            gyldigFom = LocalDate.parse("2025-10-01"),
                            gyldigTom = null
                        )
                    )
                )
            )
        )

        val resultat = beregner.tellAntallDeltakelserPerEnhet(deltakelser, ressurserMedTilknytninger)

        // Skien (123 dager) er nærmere enn Oslo (182 dager)
        assertThat(resultat.deltakelserPerEnhet).containsEntry("Skien Oppfølging ung", 1)
        assertThat(resultat.deltakelserPerEnhet).doesNotContainKey("NAV Oslo")
    }

    @Test
    fun `skal håndtere veileder med tom orgTilknytning som sikkerhetsnett`() {
        val deltakelser = listOf(
            DeltakelseInput(UUID.randomUUID(), "H155556$VEILEDER_SUFFIX", LocalDate.parse("2025-10-15"))
        )

        val ressurserMedTilknytninger = listOf(
            RessursMedAlleTilknytninger(
                navIdent = "H155556",
                orgTilknytninger = emptyList() // Har sluttet, ingen tilknytninger
            )
        )

        val resultat = beregner.tellAntallDeltakelserPerEnhet(deltakelser, ressurserMedTilknytninger)

        // Uten kobling i koblingstabellen og tom orgTilknytning → sikkerhetsnett
        assertThat(resultat.deltakelserPerEnhet).containsEntry(DeltakelsePerEnhetStatistikkTeller.ENHET_SIKKERHETSNETT, 1)
    }

}
