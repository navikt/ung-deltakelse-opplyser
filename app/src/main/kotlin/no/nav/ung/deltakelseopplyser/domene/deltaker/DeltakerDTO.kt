package no.nav.ung.deltakelseopplyser.domene.deltaker

import java.util.*

data class DeltakerDTO(
    val id: UUID ? = null,
    val deltakerIdent: String,
) {
    companion object {
        fun DeltakerDTO.mapToDAO(): DeltakerDAO {
            return DeltakerDAO(deltakerIdent = deltakerIdent)
        }
    }
}
