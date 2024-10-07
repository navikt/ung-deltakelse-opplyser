package no.nav.ung.deltakelseopplyser.register

import org.springframework.stereotype.Service
import java.util.*

@Service
class UngprogramregisterService {
    fun leggTilIProgram(deltakerProgramOpplysningDTO: DeltakerProgramOpplysningDTO): DeltakerProgramOpplysningDTO {
        TODO("Not yet implemented")
    }

    fun fjernFraProgram(id: UUID): DeltakerProgramOpplysningDTO {
        TODO("Not yet implemented")
    }

    fun oppdaterProgram(
        id: UUID,
        deltakerProgramOpplysningDTO: DeltakerProgramOpplysningDTO,
    ): DeltakerProgramOpplysningDTO {
        TODO("Not yet implemented")
    }

    fun hentFraProgram(id: UUID): DeltakerProgramOpplysningDTO {
        TODO("Not yet implemented")
    }

    fun hentAlleForDeltaker(deltakerIdent: String): DeltakerProgramOpplysningDTO {
        TODO("Not yet implemented")
    }
}
