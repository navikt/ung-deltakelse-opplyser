package no.nav.ung.deltakelseopplyser.integration.enhetsregisteret

data class OrganisasjonRespons(val navn: OrganisasjonNavnRespons?) {
    fun hentNavn(): String? {
        return navn?.sammensattnavn
    }
}
