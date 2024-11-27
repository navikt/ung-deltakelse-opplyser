package no.nav.ung.deltakelseopplyser.register

import io.hypersistence.utils.hibernate.type.range.Range
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.ung.deltakelseopplyser.integration.pdl.api.PdlService
import no.nav.ung.deltakelseopplyser.integration.ungsak.UngSakService
import no.nav.ung.sak.kontrakt.hendelser.HendelseDto
import no.nav.ung.sak.kontrakt.hendelser.HendelseInfo
import no.nav.ung.sak.kontrakt.ungdomsytelse.hendelser.UngdomsprogramOpphørHendelse
import no.nav.ung.sak.typer.AktørId
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.stereotype.Service
import org.springframework.web.ErrorResponseException
import java.time.Period
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

@Service
class UngdomsprogramregisterService(
    private val deltakelseRepository: UngdomsprogramDeltakelseRepository,
    private val deltakerRepository: UngdomsprogramDeltakerRepository,
    private val ungSakService: UngSakService,
    private val pdlService: PdlService,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(UngdomsprogramregisterService::class.java)

        fun List<UngdomsprogramDeltakelseDAO>.somDeltakelsePeriodInfo(): List<DeltakelsePeriodInfo> =
            map { deltakelseDAO ->
                DeltakelsePeriodInfo(
                    id = deltakelseDAO.id,
                    programperiodeFraOgMed = deltakelseDAO.getFom(),
                    programperiodeTilOgMed = deltakelseDAO.getTom(),
                    harSøkt = deltakelseDAO.harSøkt,
                    rapporteringsPerioder = deltakelseDAO.rapporteringsperioder()
                )
            }

        private fun UngdomsprogramDeltakelseDAO.rapporteringsperioder(): List<RapportPeriodeinfoDTO> {
            val månedEtterFomDato = getFom().plusMonths(1)
            val deltakelsetidsLinje = LocalDateTimeline(
                getFom(),
                getTom() ?: månedEtterFomDato.withDayOfMonth(månedEtterFomDato.lengthOfMonth()),
                this
            )

            val deltakelseperiodeMånedForMånedTidslinje: LocalDateTimeline<UngdomsprogramDeltakelseDAO> =
                deltakelsetidsLinje.splitAtRegular(
                    getFom().withDayOfMonth(1),
                    deltakelsetidsLinje.maxLocalDate,
                    Period.ofMonths(1)
                )

            return deltakelseperiodeMånedForMånedTidslinje.toSegments()
                .map { månedSegment: LocalDateSegment<UngdomsprogramDeltakelseDAO> ->
                    RapportPeriodeinfoDTO(
                        fraOgMed = månedSegment.fom,
                        tilOgMed = månedSegment.tom,
                        harRapportert = false,
                        inntekt = null
                    )
                }
        }
    }

    fun leggTilIProgram(deltakelseOpplysningDTO: DeltakelseOpplysningDTO): DeltakelseOpplysningDTO {
        logger.info("Legger til deltaker i programmet: $deltakelseOpplysningDTO")

        val deltakerDAO = deltakerRepository.findByDeltakerIdent(deltakelseOpplysningDTO.deltaker().deltakerIdent) ?: run {
            logger.info("Deltaker eksisterer ikke. Oppretter ny deltaker.")
            deltakerRepository.saveAndFlush(deltakelseOpplysningDTO.deltaker().mapToDAO())
        }

        val ungdomsprogramDAO = deltakelseRepository.save(deltakelseOpplysningDTO.mapToDAO(deltakerDAO))
        return ungdomsprogramDAO.mapToDTO()
    }

    fun fjernFraProgram(id: UUID): Boolean {
        logger.info("Fjerner deltaker fra programmet med id $id")
        if (!deltakelseRepository.existsById(id)) {
            logger.info("Delatker med id $id eksisterer ikke i programmet. Returnerer true")
            return true
        }

        val ungdomsprogramDAO = forsikreEksistererIProgram(id)
        deltakelseRepository.delete(ungdomsprogramDAO)

        if (deltakelseRepository.existsById(id)) {
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
        val eksiterende = forsikreEksistererIProgram(id)

        val periode = if (deltakelseOpplysningDTO.tilOgMed == null) {
            Range.closedInfinite(deltakelseOpplysningDTO.fraOgMed)
        } else {
            Range.closed(deltakelseOpplysningDTO.fraOgMed, deltakelseOpplysningDTO.tilOgMed)
        }

        val oppdatert = deltakelseRepository.save(
            eksiterende.copy(
                periode = periode,
                endretTidspunkt = ZonedDateTime.now(ZoneOffset.UTC)
            )
        )

        if (oppdatert.getTom() != null) {
            sendOpphørsHendelseTilK9(oppdatert)
        }

        return oppdatert.mapToDTO()
    }

    fun markerSomHarSøkt(id: UUID): UngdomsprogramDeltakelseDAO {
        logger.info("Markerer at deltaker har søkt programmet med id $id")
        val eksisterende = forsikreEksistererIProgram(id)
        return deltakelseRepository.save(
            eksisterende.copy(
                harSøkt = true,
                endretTidspunkt = ZonedDateTime.now(ZoneOffset.UTC)
            )
        )
    }

    private fun sendOpphørsHendelseTilK9(oppdatert: UngdomsprogramDeltakelseDAO) {
        val opphørsdato = oppdatert.getTom()
        requireNotNull(opphørsdato) { "Til og med dato må være satt for å sende inn hendelse til ung-sak" }

        logger.info("Henter aktørIder for deltaker")
        val aktørIder = pdlService.hentAktørIder(oppdatert.deltaker.deltakerIdent, historisk = true)
        val nåværendeAktørId = aktørIder.first { !it.historisk }.ident

        logger.info("Sender inn hendelse til ung-sak om at deltaker har opphørt programmet")

        val hendelsedato =
            oppdatert.endretTidspunkt?.toLocalDateTime() ?: oppdatert.opprettetTidspunkt.toLocalDateTime()
        val hendelseInfo = HendelseInfo.Builder().medOpprettet(hendelsedato)
        aktørIder.forEach {
            hendelseInfo.leggTilAktør(AktørId(it.ident))
        }

        val hendelse = UngdomsprogramOpphørHendelse(hendelseInfo.build(), opphørsdato)
        ungSakService.sendInnHendelse(
            hendelse = HendelseDto(
                hendelse,
                AktørId(nåværendeAktørId)
            )
        )
    }

    fun hentFraProgram(id: UUID): DeltakelseOpplysningDTO {
        logger.info("Henter programopplysninger for deltaker med id $id")
        val ungdomsprogramDAO = forsikreEksistererIProgram(id)
        return ungdomsprogramDAO.mapToDTO()
    }

    fun hentAlleForDeltaker(deltakerIdentEllerAktørId: String): List<DeltakelseOpplysningDTO> {
        logger.info("Henter alle programopplysninger for deltaker.")
        val identer = pdlService.hentFolkeregisteridenter(ident = deltakerIdentEllerAktørId).map { it.ident }
        val deltakerIdenter = deltakerRepository.findByDeltakerIdentIn(identer)
        val ungdomsprogramDAOs = deltakelseRepository.findByDeltaker_IdIn(deltakerIdenter.map { it.id })
        logger.info("Fant ${ungdomsprogramDAOs.size} programopplysninger for deltaker.")

        return ungdomsprogramDAOs.map { it.mapToDTO() }
    }

    fun hentAlleForDeltakerId(deltakerId: UUID): List<DeltakelseOpplysningDTO> {
        logger.info("Henter alle programopplysninger for deltaker.")
        val deltakerDAO = deltakerRepository.findById(deltakerId).orElseThrow {
            ErrorResponseException(
                HttpStatus.NOT_FOUND,
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND).also {
                    it.detail = "Fant ingen deltaker med id $deltakerId"
                },
                null
            )
        }

        val identer = pdlService.hentFolkeregisteridenter(ident = deltakerDAO.deltakerIdent).map { it.ident }
        val deltakerIdenter = deltakerRepository.findByDeltakerIdentIn(identer)
        val ungdomsprogramDAOs = deltakelseRepository.findByDeltaker_IdIn(deltakerIdenter.map { it.id })
        logger.info("Fant ${ungdomsprogramDAOs.size} programopplysninger for deltaker.")

        return ungdomsprogramDAOs.map { it.mapToDTO() }
    }

    fun hentAlleDeltakelsePerioderForDeltaker(deltakerIdentEllerAktørId: String): List<DeltakelsePeriodInfo> {
        logger.info("Henter alle programopplysninger for deltaker.")
        val identer = pdlService.hentFolkeregisteridenter(ident = deltakerIdentEllerAktørId).map { it.ident }
        val deltakerIdenter = deltakerRepository.findByDeltakerIdentIn(identer)
        val ungdomsprogramDAOs = deltakelseRepository.findByDeltaker_IdIn(deltakerIdenter.map { it.id })
        logger.info("Fant ${ungdomsprogramDAOs.size} programopplysninger for deltaker.")

        return ungdomsprogramDAOs.somDeltakelsePeriodInfo()
    }

    private fun UngdomsprogramDeltakelseDAO.mapToDTO(): DeltakelseOpplysningDTO {

        return DeltakelseOpplysningDTO(
            id = id,
            deltakerIdent = deltaker.deltakerIdent,
            deltaker = deltaker.mapToDTO(),
            harSøkt = harSøkt,
            fraOgMed = getFom(),
            tilOgMed = getTom()
        )
    }

    private fun DeltakelseOpplysningDTO.mapToDAO(deltakerDAO: DeltakerDAO): UngdomsprogramDeltakelseDAO {
        val periode = if (tilOgMed == null) {
            Range.closedInfinite(fraOgMed)
        } else {
            Range.closed(fraOgMed, tilOgMed)
        }
        return UngdomsprogramDeltakelseDAO(
            deltaker = deltakerDAO,
            periode = periode,
            harSøkt = harSøkt
        )
    }

    private fun forsikreEksistererIProgram(id: UUID): UngdomsprogramDeltakelseDAO =
        deltakelseRepository.findById(id).orElseThrow {
            ErrorResponseException(
                HttpStatus.NOT_FOUND,
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND).also {
                    it.detail = "Fant ingen deltakelse med id $id"
                },
                null
            )
        }

    private fun DeltakerDAO.mapToDTO(): DeltakerDTO {
        return DeltakerDTO(
            id = id,
            deltakerIdent = deltakerIdent
        )
    }

    private fun DeltakerDTO.mapToDAO(): DeltakerDAO {
        return DeltakerDAO(deltakerIdent = deltakerIdent)
    }
}
