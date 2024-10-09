package no.nav.ung.deltakelseopplyser.register

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.stereotype.Service
import org.springframework.web.ErrorResponseException
import java.util.*

@Service
class UngdomsprogramregisterService(private val repository: UngdomsprogramRepository) {
    private companion object {
        private val logger = LoggerFactory.getLogger(UngdomsprogramregisterService::class.java)
    }

    fun leggTilIProgram(deltakerProgramOpplysningDTO: DeltakerProgramOpplysningDTO): DeltakerProgramOpplysningDTO {
        val eksisterendeDeltakelser = hentAlleForDeltaker(deltakerProgramOpplysningDTO.deltakerIdent)
        deltakerProgramOpplysningDTO.verifiserIkkeOverlapper(eksisterendeDeltakelser)

        logger.info("Legger til deltaker i programmet: $deltakerProgramOpplysningDTO")
        val ungdomsprogramDAO = repository.save(deltakerProgramOpplysningDTO.mapToDAO())

        return ungdomsprogramDAO.mapToDTO()
    }

    fun fjernFraProgram(id: UUID): Boolean {
        logger.info("Fjerner deltaker fra programmet med id $id")
        if (!repository.existsById(id)) {
            logger.info("Delatker med id $id eksisterer ikke i programmet. Returnerer true")
            return true
        }

        val ungdomsprogramDAO = forsikreEksitererIProgram(id)
        repository.delete(ungdomsprogramDAO)

        if (repository.existsById(id)) {
            logger.error("Klarte ikke å slette deltaker fra programmet med id $id")
            throw ErrorResponseException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR).also {
                    it.detail = "Klarte ikke å slette deltaker fra programmet"
                },
                null
            )
        }

        return true
    }

    fun oppdaterProgram(
        id: UUID,
        deltakerProgramOpplysningDTO: DeltakerProgramOpplysningDTO,
    ): DeltakerProgramOpplysningDTO {
        logger.info("Oppdaterer program for deltaker med $deltakerProgramOpplysningDTO")
        val eksiterende = forsikreEksitererIProgram(id)

        val oppdatert = repository.save(
            eksiterende.copy(
                fraOgMed = deltakerProgramOpplysningDTO.fraOgMed,
                tilOgMed = deltakerProgramOpplysningDTO.tilOgMed
            )
        )
        return oppdatert.mapToDTO()
    }

    fun hentFraProgram(id: UUID): DeltakerProgramOpplysningDTO {
        logger.info("Henter programopplysninger for deltaker med id $id")
        val ungdomsprogramDAO = forsikreEksitererIProgram(id)
        return ungdomsprogramDAO.mapToDTO()
    }

    fun hentAlleForDeltaker(deltakerIdent: String): List<DeltakerProgramOpplysningDTO> {
        logger.info("Henter alle programopplysninger for deltaker.")
        val ungdomsprogramDAO = repository.findByDeltakerIdent(deltakerIdent)
        logger.info("Fant ${ungdomsprogramDAO.size} programopplysninger for deltaker.")

        return ungdomsprogramDAO.map { it.mapToDTO() }
    }

    private fun verifiserIkkeOverlapper(deltakerProgramOpplysningDTO: DeltakerProgramOpplysningDTO) {
        val eksisterendeProgram = hentAlleForDeltaker(deltakerProgramOpplysningDTO.deltakerIdent)
            .sortedWith(compareByDescending { it.fraOgMed.dayOfWeek })

        eksisterendeProgram.forEach { eksisterende ->
            val nyFra = deltakerProgramOpplysningDTO.fraOgMed
            val nyTil = deltakerProgramOpplysningDTO.tilOgMed ?: nyFra.plusYears(1)
            val eksisterendeFra = eksisterende.fraOgMed
            val eksisterendeTil = eksisterende.tilOgMed ?: eksisterendeFra.plusYears(1)

            if (!(nyTil.isBefore(eksisterendeFra) || nyFra.isAfter(eksisterendeTil))) {
                val feilmelding = "[$nyFra - $nyTil] overlapper med [$eksisterendeFra - $eksisterendeTil]"
                logger.error(feilmelding)
                throw ErrorResponseException(
                    HttpStatus.BAD_REQUEST,
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        feilmelding
                    ),
                    null
                )
            }
        }
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
