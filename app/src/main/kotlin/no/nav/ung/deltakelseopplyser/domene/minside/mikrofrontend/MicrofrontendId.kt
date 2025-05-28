package no.nav.ung.deltakelseopplyser.domene.minside.mikrofrontend

/**
 * MicrofrontendId avtales på forhånd med team-personbruker.
 */

enum class MicrofrontendId(val id: String) {

    UNGDOMSPROGRAMYTELSE_INNSYN("ungdomsprogramytelse-innsyn");

    companion object {
        private val idMap = values().associateBy(MicrofrontendId::id)

        fun fraId(id: String): MicrofrontendId =
            idMap[id] ?: throw IllegalArgumentException("Ukjent MikrofrontendId id='$id'")
    }
}
