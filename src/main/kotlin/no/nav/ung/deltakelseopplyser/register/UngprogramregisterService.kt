package no.nav.ung.deltakelseopplyser.register

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.stereotype.Service
import org.springframework.web.ErrorResponseException
import java.util.*

@Service
class UngprogramregisterService(private val repository: UngdomsprogramRepository) {

    fun leggTilIProgram(deltakerProgramOpplysningDTO: DeltakerProgramOpplysningDTO): DeltakerProgramOpplysningDTO {
        val ungdomsprogramDAO = repository.save(deltakerProgramOpplysningDTO.mapToDAO())
        return ungdomsprogramDAO.mapToDTO()
    }

    fun fjernFraProgram(id: UUID): Boolean {
        val ungdomsprogramDAO = forsikreEksitererIProgram(id)
        repository.delete(ungdomsprogramDAO)
        val eksisterer = repository.existsById(id)
        if (eksisterer) throw ErrorResponseException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR).also {
                it.detail = "Klarte ikke å slette deltaker fra programmet"
            },
            null
        )

        return true
    }

    fun oppdaterProgram(
        id: UUID,
        deltakerProgramOpplysningDTO: DeltakerProgramOpplysningDTO,
    ): DeltakerProgramOpplysningDTO {
        val ungdomsprogramDAO = forsikreEksitererIProgram(id)

        val updatedUngdomsprogramDAO = repository.save(
            ungdomsprogramDAO.copy(
                fraOgMed = deltakerProgramOpplysningDTO.fraOgMed,
                tilOgMed = deltakerProgramOpplysningDTO.tilOgMed
            )
        )
        return updatedUngdomsprogramDAO.mapToDTO()
    }

    fun hentFraProgram(id: UUID): DeltakerProgramOpplysningDTO {
        val ungdomsprogramDAO = forsikreEksitererIProgram(id)
        return ungdomsprogramDAO.mapToDTO()
    }

    fun hentAlleForDeltaker(deltakerIdent: String): List<DeltakerProgramOpplysningDTO> {
        val ungdomsprogramDAO = repository.findByDeltakerIdent(deltakerIdent)
        if (ungdomsprogramDAO.isNullOrEmpty()) {
            throw ErrorResponseException(
                HttpStatus.NOT_FOUND,
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND).also {
                    it.detail = "Fant ingen programopplysninger for deltaker med ident $deltakerIdent"
                },
                null
            )
        }

        return ungdomsprogramDAO.map { it.mapToDTO() }
    }

    private fun UngdomsprogramDAO.mapToDTO(): DeltakerProgramOpplysningDTO {
        return DeltakerProgramOpplysningDTO(
            id = id,
            deltakerIdent = deltakerIdent,
            fraOgMed = fraOgMed,
            tilOgMed = tilOgMed
        )
    }

    private fun DeltakerProgramOpplysningDTO.mapToDAO(): UngdomsprogramDAO {
        return UngdomsprogramDAO(
            deltakerIdent = deltakerIdent,
            fraOgMed = fraOgMed,
            tilOgMed = tilOgMed
        )
    }

    private fun forsikreEksitererIProgram(id: UUID): UngdomsprogramDAO =
        repository.findById(id).orElseThrow {
            ErrorResponseException(
                HttpStatus.NOT_FOUND,
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND).also {
                    it.detail = "Fant programopplysning for deltaker med id $id"
                },
                null
            )
        }
}
