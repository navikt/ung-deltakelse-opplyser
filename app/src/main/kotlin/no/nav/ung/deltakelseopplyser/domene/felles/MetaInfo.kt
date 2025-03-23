package no.nav.ung.deltakelseopplyser.domene.felles

data class MetaInfo(
    val version: Int = 1,
    val correlationId: String,
    val soknadDialogCommitSha: String? = null,
)
