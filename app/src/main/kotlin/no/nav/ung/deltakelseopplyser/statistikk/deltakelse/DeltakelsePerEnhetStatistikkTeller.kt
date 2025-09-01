package no.nav.ung.deltakelseopplyser.statistikk.deltakelse

import no.nav.ung.deltakelseopplyser.historikk.AuditorAwareImpl.Companion.VEILEDER_SUFFIX
import no.nav.ung.deltakelseopplyser.integration.nom.api.NomApiService
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

data class DeltakelseInput(
    val id: UUID,
    val opprettetAv: String,
    val opprettetDato: LocalDate,
)

data class DeltakelsePerEnhetResultat(
    val deltakelserPerEnhet: Map<String, Int>,
    val diagnostikk: Map<Any, Any?> = emptyMap(),
)

class DeltakelsePerEnhetStatistikkTeller {
    private companion object {
        private val logger = LoggerFactory.getLogger(DeltakelsePerEnhetStatistikkTeller::class.java)
    }

    fun tellAntallDeltakelserPerEnhet(
        deltakelser: List<DeltakelseInput>,
        ressurserMedTilknytninger: List<NomApiService.RessursMedAlleTilknytninger>,
    ): DeltakelsePerEnhetResultat {

        // Bygg lookup-map for effektiv søking
        val ressursLookup = ressurserMedTilknytninger.associateBy { it.navIdent }

        // For diagnostikk - spor veiledere med flere enheter
        val veiledereMedFlereEnheter = mutableMapOf<String, List<NomApiService.OrgEnhetMedPeriode>>()

        // Map hver deltakelse individuelt til enhetsnavn, med periodefiltrering per deltakelse
        val deltakelserPerEnhet = deltakelser
            .mapNotNull { deltakelse ->
                val navIdent = deltakelse.opprettetAv.replace(VEILEDER_SUFFIX, "").trim()
                val ressurs = ressursLookup[navIdent]

                if (ressurs != null) {
                    // Finn gyldige enheter for denne spesifikke deltakelsesdatoen
                    val gyldigeEnheter = ressurs.orgTilknytninger
                        .filter { tilknytning -> tilknytning.erGyldigPåTidspunkt(deltakelse.opprettetDato) }
                        .map { tilknytning -> tilknytning.orgEnhet }
                        .filter { enhet -> enhet.erGyldigPåTidspunkt(deltakelse.opprettetDato) }
                        .distinctBy { "${it.id}-${it.navn}" }

                    if (gyldigeEnheter.isNotEmpty()) {
                        val valgtEnhet = if (gyldigeEnheter.size == 1) {
                            gyldigeEnheter.first()
                        } else {
                            // Ved flere enheter, beregn popularitet på tvers av alle ressurser
                            // for å gjøre et konsistent valg
                            val enheterMedFlestForekomsterBlandVeilederne =
                                enhetMedFlesForekomsterBlandVeilederne(ressurserMedTilknytninger)

                            val mestSannsynligeEnhet = gyldigeEnheter
                                .maxByOrNull { enhet ->
                                    enheterMedFlestForekomsterBlandVeilederne["${enhet.id}-${enhet.navn}"] ?: 0
                                }
                                ?: gyldigeEnheter.first()

                            logger.warn(
                                "NAV-ident hadde ${gyldigeEnheter.size} enheter på tidspunkt ${deltakelse.opprettetDato} [${gyldigeEnheter.map { "${it.id}-${it.navn} (popularitet: ${enheterMedFlestForekomsterBlandVeilederne["${it.id}-${it.navn}"] ?: 0})" }}], valgte den mest populære: ${mestSannsynligeEnhet.id}-${mestSannsynligeEnhet.navn}"
                            )

                            // Legg til i diagnostikk-data
                            veiledereMedFlereEnheter[navIdent] = gyldigeEnheter

                            mestSannsynligeEnhet
                        }

                        valgtEnhet.navn
                    } else {
                        logger.warn("Fant ingen gyldig enhet for NAV-ident $navIdent på tidspunkt ${deltakelse.opprettetDato}")
                        null
                    }
                } else {
                    logger.warn("Fant ingen ressurs for NAV-ident $navIdent")
                    null
                }
            }
            .groupingBy { it }
            .eachCount()

        val antallUnikeNavIdenter = deltakelser.map { it.opprettetAv.replace(VEILEDER_SUFFIX, "").trim() }.toSet().size
        val totalAntallDeltakelser = deltakelser.size

        // Beregn popularitet for diagnostikk
        val enhetPopularitet = enhetMedFlesForekomsterBlandVeilederne(ressurserMedTilknytninger)

        logger.info(
            "Fant {} unike NAV-identer fra {} deltakelser",
            antallUnikeNavIdenter,
            totalAntallDeltakelser
        )

        return DeltakelsePerEnhetResultat(
            deltakelserPerEnhet = deltakelserPerEnhet,
            diagnostikk = mapOf(
                "enhetPopularitet" to enhetPopularitet,
                "veiledereMedFlereEnheter" to veiledereMedFlereEnheter,
                "totalAntallDeltakelser" to totalAntallDeltakelser,
                "antallUnikeNavIdenter" to antallUnikeNavIdenter
            )
        )
    }

    private fun enhetMedFlesForekomsterBlandVeilederne(ressurserMedTilknytninger: List<NomApiService.RessursMedAlleTilknytninger>): Map<String, Int> {
        val enhetPopularitet = ressurserMedTilknytninger
            .flatMap { it.orgTilknytninger }
            .map { it.orgEnhet }
            .groupingBy { "${it.id}-${it.navn}" }
            .eachCount()
        return enhetPopularitet
    }
}
