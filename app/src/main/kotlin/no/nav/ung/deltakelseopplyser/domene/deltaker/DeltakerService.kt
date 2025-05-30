package no.nav.ung.deltakelseopplyser.domene.deltaker

import no.nav.pdl.generated.hentperson.Foedselsdato
import no.nav.pdl.generated.hentperson.Person
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveDAO
import no.nav.ung.deltakelseopplyser.integration.kontoregister.KontoregisterService
import no.nav.ung.deltakelseopplyser.integration.pdl.api.PdlService
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.KontonummerDTO
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseOpplysningDTO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
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
    private val kontoregisterService: KontoregisterService,
    @Value("\${PROGRAM_OPPSTART_DATO}") private val programOppstartdato: String? = null,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DeltakerService::class.java)
        fun DeltakerDTO.mapToDAO(): DeltakerDAO {
            return DeltakerDAO(deltakerIdent = deltakerIdent)
        }

        fun DeltakerDAO.mapToDTO(): DeltakerDTO {
            return DeltakerDTO(
                id = id,
                deltakerIdent = deltakerIdent
            )
        }
    }

    fun hentDeltakerInfo(deltakerId: UUID? = null, deltakerIdent: String? = null): DeltakerPersonalia? {
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

    fun hentDeltakterIdenter(deltakerIdentEllerAktørId: String): List<String> {
        return hentDeltakere(deltakerIdentEllerAktørId).map { it.deltakerIdent }
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

    fun oppdaterDeltaker(deltaker: DeltakerDAO): DeltakerDAO {
        return deltakerRepository.save(deltaker)
    }

    fun hentDeltakersOppgaver(deltakerIdentEllerAktørId: String): List<OppgaveDAO> {
        logger.info("Henter deltakers oppgaver")
        val oppgaver = hentDeltakere(deltakerIdentEllerAktørId).flatMap { it.oppgaver }
        logger.info("Fant ${oppgaver.size} oppgaver for deltaker.")
        return oppgaver
    }

    fun finnDeltakerGittOppgaveReferanse(oppgaveReferanse: UUID): DeltakerDAO? {
        return deltakerRepository.finnDeltakerGittOppgaveReferanse(oppgaveReferanse)
    }

    private fun hentDeltakerInfoMedId(id: UUID): DeltakerPersonalia {
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

        val pdlPerson = hentPdlPerson(deltakerDAO.deltakerIdent)

        return DeltakerPersonalia(
            id = deltakerDAO?.id,
            deltakerIdent = deltakerDAO.deltakerIdent,
            navn = pdlPerson.navn.first(),
            fødselsdato = pdlPerson.foedselsdato.first().toLocalDate(),
            programOppstartdato = programOppstartdato?.let { LocalDate.parse(it) }
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

    private fun hentDeltakerInfoMedIdent(deltakerIdent: String): DeltakerPersonalia {
        val deltakerDAO = deltakerRepository.findByDeltakerIdent(deltakerIdent)

        val PdlPerson = hentPdlPerson(deltakerDAO?.deltakerIdent ?: deltakerIdent)

        return DeltakerPersonalia(
            id = deltakerDAO?.id,
            deltakerIdent = deltakerIdent,
            navn = PdlPerson.navn.first(),
            fødselsdato = PdlPerson.foedselsdato.first().toLocalDate(),
            programOppstartdato = programOppstartdato?.let { LocalDate.parse(it) }
        )
    }

    private fun Foedselsdato.toLocalDate(): LocalDate {
        return LocalDate.parse(foedselsdato.toString())
    }

    fun hentKontonummer(): KontonummerDTO {
        return kontoregisterService.hentAktivKonto()
    }

    fun slettDeltaker(deltakerId: UUID): Boolean {
        deltakerRepository.deleteById(deltakerId)
        return !deltakerRepository.existsById(deltakerId)
    }
}
