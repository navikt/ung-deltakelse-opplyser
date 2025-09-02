package no.nav.ung.deltakelseopplyser.integration.nom.api.domene

import java.time.LocalDate

data class OrgEnhetMedPeriode(
    val id: String,
    val navn: String,
    val gyldigFom: LocalDate,
    val gyldigTom: LocalDate?,
    ) {
        /**
         * Sjekker om enheten var gyldig på et spesifikt tidspunkt.
         */
        fun erGyldigPåTidspunkt(tidspunkt: LocalDate): Boolean {
            val startetFørEllerPå = !gyldigFom.isAfter(tidspunkt)
            val ikkeSlutetFør = gyldigTom == null || !gyldigTom.isBefore(tidspunkt)

            return startetFørEllerPå && ikkeSlutetFør
        }
    }
