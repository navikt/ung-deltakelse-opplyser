package no.nav.ung.deltakelseopplyser.register

import no.nav.pdl.generated.hentperson.Navn
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

    fun hentDeltakerInfo(deltakerDTO: DeltakerDTO): DeltakerPersonlia? {
        val deltakerDAO = deltakerRepository.findByDeltakerIdent(deltakerDTO.deltakerIdent)

        val PdlPerson = kotlin.runCatching { pdlService.hentPerson(deltakerDTO.deltakerIdent) }
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

        return DeltakerPersonlia(
            id = deltakerDAO?.id,
            deltakerIdent = deltakerDTO.deltakerIdent,
            navn = PdlPerson.navn.first()
        )
    }

    data class DeltakerPersonlia(
        val id: UUID? = null,
        val deltakerIdent: String,
        val navn: Navn,
    )
}
