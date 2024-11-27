package no.nav.ung.deltakelseopplyser.register

import no.nav.pdl.generated.hentperson.Navn
import no.nav.pdl.generated.hentperson.Person
import no.nav.ung.deltakelseopplyser.integration.pdl.api.PdlService
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.stereotype.Service
import org.springframework.web.ErrorResponseException
import java.util.*

@Service
class DeltakerInfoService(
    private val deltakerRepository: UngdomsprogramDeltakerRepository,
    private val pdlService: PdlService,
) {

    fun hentDeltakerInfo(deltakerId: UUID? = null, deltakerIdent: String? = null): DeltakerPersonlia? {
        return when {
            deltakerId != null -> hentDeltakerInfoMedId(deltakerId)
            deltakerIdent != null -> hentDeltakerInfoMedIdent(deltakerIdent)
            else -> {
                throw ErrorResponseException(
                    HttpStatus.BAD_REQUEST,
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        "Mangler deltakerId eller deltakerIdent"
                    ),
                    null
                )
            }
        }
    }

    private fun hentDeltakerInfoMedIdent(deltakerIdent: String): DeltakerPersonlia? {
        val deltakerDAO = deltakerRepository.findByDeltakerIdent(deltakerIdent)

        val PdlPerson = hentPdlPerson(deltakerDAO?.deltakerIdent ?: deltakerIdent)

        return DeltakerPersonlia(
            id = deltakerDAO?.id,
            deltakerIdent = deltakerIdent,
            navn = PdlPerson.navn.first()
        )
    }

    private fun hentDeltakerInfoMedId(id: UUID): DeltakerPersonlia? {
        val deltakerDAO = deltakerRepository.findById(id).orElseThrow {
            ErrorResponseException(
                HttpStatus.NOT_FOUND,
                ProblemDetail.forStatusAndDetail(
                    HttpStatus.NOT_FOUND,
                    "Fant ikke deltaker med id $id"
                ),
                null
            )
        }

        val PdlPerson = hentPdlPerson(deltakerDAO.deltakerIdent)

        return DeltakerPersonlia(
            id = deltakerDAO?.id,
            deltakerIdent = deltakerDAO.deltakerIdent,
            navn = PdlPerson.navn.first()
        )
    }

    private fun hentPdlPerson(deltakerIdent: String): Person {
        val PdlPerson = kotlin.runCatching { pdlService.hentPerson(deltakerIdent) }
            .fold(
                onSuccess = { it },
                onFailure = {
                    throw ErrorResponseException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        ProblemDetail.forStatusAndDetail(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Feil ved henting av person"
                        ),
                        it
                    )
                }
            )
        return PdlPerson
    }

    data class DeltakerPersonlia(
        val id: UUID? = null,
        val deltakerIdent: String,
        val navn: Navn,
    )
}
