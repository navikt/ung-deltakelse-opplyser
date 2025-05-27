package no.nav.ung.deltakelseopplyser.domene.minside.mikrofrontend

/**
 * MicrofrontendId avtales på forhånd med team-personbruker.
 */

enum class MikrofrontendId(val id: String) {

    UNGDOMSPROGRAMYTELSE_INNSYN("ungdomsprogramytelse-innsyn");

    companion object {
        private val idMap = values().associateBy(MikrofrontendId::id)

        fun fraId(id: String): MikrofrontendId =
            idMap[id] ?: throw IllegalArgumentException("Ukjent MikrofrontendId id='$id'")
    }
}
