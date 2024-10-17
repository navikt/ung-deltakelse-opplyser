package no.nav.ung.deltakelseopplyser.register

import no.nav.k9.sak.kontrakt.hendelser.HendelseDto
import no.nav.k9.sak.kontrakt.hendelser.HendelseInfo
import no.nav.k9.sak.kontrakt.ungdomsytelse.hendelser.UngdomsprogramOpphørHendelse
import no.nav.k9.sak.typer.AktørId
import no.nav.ung.deltakelseopplyser.integration.k9sak.K9SakService
import no.nav.ung.deltakelseopplyser.integration.pdl.PdlService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.stereotype.Service
import org.springframework.web.ErrorResponseException
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

@Service
class UngdomsprogramregisterService(
    private val repository: UngdomsprogramDeltakelseRepository,
    private val k9SakService: K9SakService,
    private val pdlService: PdlService,
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(UngdomsprogramregisterService::class.java)
    }

    fun leggTilIProgram(deltakelseOpplysningDTO: DeltakelseOpplysningDTO): DeltakelseOpplysningDTO {
        val eksisterendeDeltakelser = hentAlleForDeltaker(deltakelseOpplysningDTO.deltakerIdent)
        deltakelseOpplysningDTO.verifiserIkkeOverlapper(eksisterendeDeltakelser)

        logger.info("Legger til deltaker i programmet: $deltakelseOpplysningDTO")
        val ungdomsprogramDAO = repository.save(deltakelseOpplysningDTO.mapToDAO())

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
        deltakelseOpplysningDTO: DeltakelseOpplysningDTO,
    ): DeltakelseOpplysningDTO {
        logger.info("Oppdaterer program for deltaker med $deltakelseOpplysningDTO")
        val eksiterende = forsikreEksitererIProgram(id)

        val oppdatert = repository.save(
            eksiterende.copy(
                fraOgMed = deltakelseOpplysningDTO.fraOgMed,
                tilOgMed = deltakelseOpplysningDTO.tilOgMed,
                endretTidspunkt = ZonedDateTime.now(ZoneOffset.UTC)
            )
        )

        if (oppdatert.tilOgMed != null) {
            sendOpphørsHendelseTilK9(oppdatert)
        }

        return oppdatert.mapToDTO()
    }

    private fun sendOpphørsHendelseTilK9(oppdatert: UngdomsprogramDeltakelseDAO) {
        val opphørsdato = oppdatert.tilOgMed
        requireNotNull(opphørsdato) { "Til og med dato må være satt for å sende inn hendelse til k9-sak" }

        logger.info("Henter aktørIder for deltaker")
        val aktørIder = pdlService.hentAktørIder(oppdatert.deltakerIdent, historisk = true)
        val nåværendeAktørId = aktørIder.first { !it.historisk }.ident

        logger.info("Sender inn hendelse til k9-sak om at deltaker har opphørt programmet")

        val hendelsedato =
            oppdatert.endretTidspunkt?.toLocalDateTime() ?: oppdatert.opprettetTidspunkt.toLocalDateTime()
        val hendelseInfo = HendelseInfo.Builder().medOpprettet(hendelsedato)
        aktørIder.forEach {
            hendelseInfo.leggTilAktør(AktørId(it.ident))
        }

        val hendelse = UngdomsprogramOpphørHendelse(hendelseInfo.build(), opphørsdato)
        k9SakService.sendInnHendelse(
            hendelse = HendelseDto(
                hendelse,
                AktørId(nåværendeAktørId)
            )
        )
    }

    fun hentFraProgram(id: UUID): DeltakelseOpplysningDTO {
        logger.info("Henter programopplysninger for deltaker med id $id")
        val ungdomsprogramDAO = forsikreEksitererIProgram(id)
        return ungdomsprogramDAO.mapToDTO()
    }

    fun hentAlleForDeltaker(deltakerIdent: String): List<DeltakelseOpplysningDTO> {
        logger.info("Henter alle programopplysninger for deltaker.")
        val ungdomsprogramDAO = repository.findByDeltakerIdent(deltakerIdent)
        logger.info("Fant ${ungdomsprogramDAO.size} programopplysninger for deltaker.")

        return ungdomsprogramDAO.map { it.mapToDTO() }
    }

    private fun UngdomsprogramDeltakelseDAO.mapToDTO(): DeltakelseOpplysningDTO {
        return DeltakelseOpplysningDTO(
            id = id,
            deltakerIdent = deltakerIdent,
            fraOgMed = fraOgMed,
            tilOgMed = tilOgMed
        )
    }

    private fun DeltakelseOpplysningDTO.mapToDAO(): UngdomsprogramDeltakelseDAO {
        return UngdomsprogramDeltakelseDAO(
            deltakerIdent = deltakerIdent,
            fraOgMed = fraOgMed,
            tilOgMed = tilOgMed
        )
    }

    private fun forsikreEksitererIProgram(id: UUID): UngdomsprogramDeltakelseDAO =
        repository.findById(id).orElseThrow {
            ErrorResponseException(
                HttpStatus.NOT_FOUND,
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND).also {
                    it.detail = "Fant ingen deltakelse med id $id"
                },
                null
            )
        }
}
