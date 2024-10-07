package no.nav.ung.deltakelseopplyser.register

import org.apache.kafka.common.protocol.types.Field.UUID
import java.time.LocalDate

data class DeltakerProgramOpplysningDTO(
    val id: UUID,
    val deltakerIdent: String,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate? = null,
)
