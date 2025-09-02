package no.nav.ung.deltakelseopplyser.integration.nom.api.domene

import no.nav.nom.generated.hentressurser.OrgEnhet

data class RessursMedEnheter(val navIdent: String, val enheter: List<OrgEnhet>)
