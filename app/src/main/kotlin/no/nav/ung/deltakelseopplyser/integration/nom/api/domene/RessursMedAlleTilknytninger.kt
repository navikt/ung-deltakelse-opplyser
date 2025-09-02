package no.nav.ung.deltakelseopplyser.integration.nom.api.domene

data class RessursMedAlleTilknytninger(
        val navIdent: String,
        val orgTilknytninger: List<RessursOrgTilknytningMedPeriode>,
    )
