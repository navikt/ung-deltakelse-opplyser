package no.nav.ung.deltakelseopplyser.statistikk.deltakelse

import no.nav.ung.deltakelseopplyser.historikk.AuditorAwareImpl.Companion.VEILEDER_SUFFIX
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

data class DeltakelseInput(
    val id: UUID,
    val opprettetAv: String,
    val opprettetDato: LocalDate
)

data class RessursMedEnheterInput(
    val navIdent: String,
    val enheter: List<OrgEnhetInput>
)

data class OrgEnhetInput(
    val id: String,
    val navn: String
)

data class DeltakelsePerEnhetResultat(
    val deltakelserPerEnhet: Map<String, Int>,
    val diagnostikk: Map<Any, Any?> = emptyMap()
)

class DeltakelseStatistikkBeregner {
    private companion object {
        private val logger = LoggerFactory.getLogger(DeltakelseStatistikkBeregner::class.java)
    }

    fun beregnAntallDeltakelserPerEnhet(
        deltakelser: List<DeltakelseInput>,
        ressurserMedEnheter: List<RessursMedEnheterInput>
    ): DeltakelsePerEnhetResultat {

        // Bygg lookup-map for effektiv søking
        val ressursLookup = ressurserMedEnheter.associateBy { it.navIdent }

        // Beregn vektlegging av enheter basert på hvor ofte de forekommer blant alle veiledere
        val enhetPopularitet = ressurserMedEnheter
            .flatMap { it.enheter }
            .groupingBy { "${it.id}-${it.navn}" }
            .eachCount()

        logger.info("Beregnet popularitet for {} enheter", enhetPopularitet.size)

        // For diagnostikk - spor veiledere med flere enheter
        val veiledereMedFlereEnheter = mutableMapOf<String, List<OrgEnhetInput>>()

        // Map hver deltakelse til enhetsnavn
        val deltakelserPerEnhet = deltakelser
            .mapNotNull { deltakelse ->
                val navIdent = deltakelse.opprettetAv.replace(VEILEDER_SUFFIX, "").trim()
                val ressursMedEnheter = ressursLookup[navIdent]

                if (ressursMedEnheter?.enheter?.isNotEmpty() == true) {
                    val enheter = ressursMedEnheter.enheter

                    val valgtEnhet = if (enheter.size == 1) {
                        enheter.first()
                    } else {
                        // Velg enheten med høyest popularitet (flest forekomster)
                        val mestPopulaereEnhet = enheter
                            .maxByOrNull { enhet ->
                                enhetPopularitet["${enhet.id}-${enhet.navn}"] ?: 0
                            }
                            ?: enheter.first()

                        logger.warn(
                            "NAV-ident hadde ${enheter.size} enheter på tidspunkt ${deltakelse.opprettetDato} [${enheter.map { "${it.id}-${it.navn} (popularitet: ${enhetPopularitet["${it.id}-${it.navn}"] ?: 0})" }}], valgte den mest populære: ${mestPopulaereEnhet.id}-${mestPopulaereEnhet.navn}"
                        )

                        // Legg til i diagnostikk-data
                        veiledereMedFlereEnheter[navIdent] = enheter

                        mestPopulaereEnhet
                    }

                    valgtEnhet.navn
                } else {
                    logger.warn("Fant ingen gyldig enhet for NAV-ident $navIdent på tidspunkt ${deltakelse.opprettetDato}")
                    null
                }
            }
            .groupingBy { it }
            .eachCount()

        val antallUnikeNavIdenter = deltakelser.map { it.opprettetAv.replace(VEILEDER_SUFFIX, "").trim() }.toSet().size
        val totalAntallDeltakelser = deltakelser.size

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
}
