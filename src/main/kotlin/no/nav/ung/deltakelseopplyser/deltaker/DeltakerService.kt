package no.nav.ung.deltakelseopplyser.deltaker

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.pdl.generated.hentperson.Foedselsdato
import no.nav.pdl.generated.hentperson.Navn
import no.nav.pdl.generated.hentperson.Person
import no.nav.ung.deltakelseopplyser.deltaker.DeltakerDTO.Companion.mapToDAO
import no.nav.ung.deltakelseopplyser.integration.pdl.api.PdlService
import no.nav.ung.deltakelseopplyser.register.DeltakelseOpplysningDTO
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.stereotype.Service
import org.springframework.web.ErrorResponseException
import java.time.LocalDate
import java.util.*

@Service
class DeltakerService(
    private val deltakerRepository: DeltakerRepository,
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

    fun hentDeltakterIder(deltakerIdentEllerAktørId: String): List<UUID> {
        return hentDeltakere(deltakerIdentEllerAktørId).map { it.id }
    }

    fun finnDeltakerGittIdent(deltakerIdent: String): DeltakerDAO? {
        return deltakerRepository.findByDeltakerIdent(deltakerIdent)
    }

    fun finnDeltakerGittId(id: UUID): Optional<DeltakerDAO> {
        return deltakerRepository.findById(id)
    }

    fun lagreDeltaker(deltakelseOpplysningDTO: DeltakelseOpplysningDTO): DeltakerDAO {
        return deltakerRepository.saveAndFlush(deltakelseOpplysningDTO.deltaker.mapToDAO())
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
            navn = PdlPerson.navn.first(),
            fødselsdato = PdlPerson.foedselsdato.first().toLocalDate()
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

    private fun hentDeltakere(deltakerIdentEllerAktørId: String): List<DeltakerDAO> {
        val identer = pdlService.hentFolkeregisteridenter(ident = deltakerIdentEllerAktørId).map { it.ident }
        return deltakerRepository.findByDeltakerIdentIn(identer)
    }

    private fun hentDeltakerInfoMedIdent(deltakerIdent: String): DeltakerPersonlia? {
        val deltakerDAO = deltakerRepository.findByDeltakerIdent(deltakerIdent)

        val PdlPerson = hentPdlPerson(deltakerDAO?.deltakerIdent ?: deltakerIdent)

        return DeltakerPersonlia(
            id = deltakerDAO?.id,
            deltakerIdent = deltakerIdent,
            navn = PdlPerson.navn.first(),
            fødselsdato = PdlPerson.foedselsdato.first().toLocalDate()
        )
    }

    private fun Foedselsdato.toLocalDate(): LocalDate {
        return LocalDate.parse(foedselsdato.toString())
    }

    data class DeltakerPersonlia(
        val id: UUID? = null,
        val deltakerIdent: String,
        val navn: Navn,
        val fødselsdato: LocalDate,
    ) {
        @get:JsonProperty("førsteMuligeInnmeldingsdato")
        val førsteMuligeInnmeldingsdato: LocalDate
            get() = fødselsdato.plusYears(18)

        @get:JsonProperty("sisteMuligeInnmeldingsdato")
        val sisteMuligeInnmeldingsdato: LocalDate
            get() = fødselsdato.plusYears(29)
    }
}
