package no.nav.ung.deltakelseopplyser.domene.register

import io.hypersistence.utils.hibernate.type.range.Range
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDAO
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDTO
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.oppgave.OppgaveDTO
import no.nav.ung.deltakelseopplyser.domene.oppgave.OppgaveDTO.Companion.tilDTO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.EndretSluttdatoOppgavetypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.EndretStartdatoOppgavetypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveStatus
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.Oppgavetype
import no.nav.ung.deltakelseopplyser.domene.register.veileder.EndrePeriodeDatoDTO
import no.nav.ung.deltakelseopplyser.integration.pdl.api.PdlService
import no.nav.ung.deltakelseopplyser.integration.ungsak.UngSakService
import no.nav.ung.sak.kontrakt.hendelser.HendelseDto
import no.nav.ung.sak.kontrakt.hendelser.HendelseInfo
import no.nav.ung.sak.kontrakt.hendelser.UngdomsprogramEndretStartdatoHendelse
import no.nav.ung.sak.kontrakt.hendelser.UngdomsprogramOpphørHendelse
import no.nav.ung.sak.typer.AktørId
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.stereotype.Service
import org.springframework.web.ErrorResponseException
import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

@Service
class UngdomsprogramregisterService(
    private val deltakelseRepository: UngdomsprogramDeltakelseRepository,
    private val deltakerService: DeltakerService,
    private val ungSakService: UngSakService,
    private val pdlService: PdlService,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(UngdomsprogramregisterService::class.java)

        fun List<UngdomsprogramDeltakelseDAO>.somDeltakelsePeriodInfo(): List<DeltakelsePeriodInfo> =
            map { deltakelseDAO ->
                DeltakelsePeriodInfo(
                    id = deltakelseDAO.id,
                    fraOgMed = deltakelseDAO.getFom(),
                    tilOgMed = deltakelseDAO.getTom(),
                    harSøkt = deltakelseDAO.harSøkt,
                    rapporteringsPerioder = deltakelseDAO.rapporteringsperioder(),
                    oppgaver = deltakelseDAO.oppgaver.map { it.tilDTO() }
                )
            }

        private fun UngdomsprogramDeltakelseDAO.rapporteringsperioder(): List<RapportPeriodeinfoDTO> {
            val deltakelsetidsLinje = LocalDateTimeline(
                getFom(),
                getTom() ?: LocalDate.now(),
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

        val deltakerDAO = deltakerService.finnDeltakerGittIdent(deltakelseOpplysningDTO.deltaker.deltakerIdent) ?: run {
            logger.info("Deltaker eksisterer ikke. Oppretter ny deltaker.")
            deltakerService.lagreDeltaker(deltakelseOpplysningDTO)
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

        eksiterende.oppdaterPeriode(periode)

        if (eksiterende.getTom() != null) {
            sendEndretSluttdatoHendelseTilUngSak(eksiterende)
        }

        return deltakelseRepository.save(eksiterende).mapToDTO()
    }

    fun markerSomHarSøkt(id: UUID): DeltakelseOpplysningDTO {
        logger.info("Markerer at deltaker har søkt programmet med id $id")
        val eksisterende = forsikreEksistererIProgram(id)
        eksisterende.markerSomHarSøkt()
        return deltakelseRepository.save(eksisterende).mapToDTO()
    }

    fun hentFraProgram(id: UUID): DeltakelseOpplysningDTO {
        logger.info("Henter programopplysninger for deltaker med id $id")
        val ungdomsprogramDAO = forsikreEksistererIProgram(id)
        return ungdomsprogramDAO.mapToDTO()
    }

    fun hentAlleForDeltaker(deltakerIdentEllerAktørId: String): List<DeltakelseOpplysningDTO> {
        logger.info("Henter alle programopplysninger for deltaker.")
        val deltakerIder = deltakerService.hentDeltakterIder(deltakerIdentEllerAktørId)
        val ungdomsprogramDAOs = deltakelseRepository.findByDeltaker_IdIn(deltakerIder)
        logger.info("Fant ${ungdomsprogramDAOs.size} programopplysninger for deltaker.")

        return ungdomsprogramDAOs.map { it.mapToDTO() }
    }

    fun hentAlleForDeltakerId(deltakerId: UUID): List<DeltakelseOpplysningDTO> {
        logger.info("Henter alle programopplysninger for deltaker.")
        val deltakerDAO = deltakerService.finnDeltakerGittId(deltakerId).orElseThrow {
            ErrorResponseException(
                HttpStatus.NOT_FOUND,
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND).also {
                    it.detail = "Fant ingen deltaker med id $deltakerId"
                },
                null
            )
        }

        val deltakterIder = deltakerService.hentDeltakterIder(deltakerDAO.deltakerIdent)
        val ungdomsprogramDAOs = deltakelseRepository.findByDeltaker_IdIn(deltakterIder)
        logger.info("Fant ${ungdomsprogramDAOs.size} programopplysninger for deltaker.")

        return ungdomsprogramDAOs.map { it.mapToDTO() }
    }

    fun hentAlleDeltakelsePerioderForDeltaker(deltakerIdentEllerAktørId: String): List<DeltakelsePeriodInfo> {
        logger.info("Henter alle programopplysninger for deltaker.")

        val deltakterIder = deltakerService.hentDeltakterIder(deltakerIdentEllerAktørId)
        val ungdomsprogramDAOs = deltakelseRepository.findByDeltaker_IdIn(deltakterIder)
        logger.info("Fant ${ungdomsprogramDAOs.size} programopplysninger for deltaker.")

        return ungdomsprogramDAOs.somDeltakelsePeriodInfo()
    }

    fun avsluttDeltakelse(
        id: UUID,
        deltakelseOpplysningDTO: DeltakelseOpplysningDTO,
    ): DeltakelseOpplysningDTO {
        logger.info("Avsluttr deltakelse i program for deltaker med $deltakelseOpplysningDTO")
        val eksiterende = forsikreEksistererIProgram(id)

        val periode = if (deltakelseOpplysningDTO.tilOgMed == null) {
            Range.closedInfinite(deltakelseOpplysningDTO.fraOgMed)
        } else {
            Range.closed(deltakelseOpplysningDTO.fraOgMed, deltakelseOpplysningDTO.tilOgMed)
        }

        eksiterende.oppdaterPeriode(periode)
        val oppdatert = deltakelseRepository.save(eksiterende)

        if (oppdatert.getTom() != null) {
            sendEndretSluttdatoHendelseTilUngSak(oppdatert)
        }

        return oppdatert.mapToDTO()
    }

    fun endreStartdato(deltakelseId: UUID, endrePeriodeDatoDTO: EndrePeriodeDatoDTO): DeltakelseOpplysningDTO {
        val eksisterende = forsikreEksistererIProgram(deltakelseId)
        logger.info("Endrer startdato for deltakelse med id $deltakelseId fra ${eksisterende.getFom()} til $endrePeriodeDatoDTO")

        val startdato = endrePeriodeDatoDTO.dato
        val sluttdato = eksisterende.getTom()
        forsikreGyldigPeriode(sluttdato, startdato)

        eksisterende.oppgaver.find { it.oppgavetype == Oppgavetype.BEKREFT_ENDRET_STARTDATO && it.status == OppgaveStatus.ULØST }
            ?.apply {
                logger.info("Fant uløst oppgave for endring av startdato. Markerer som kansellert.")
                markerSomKansellert()
                eksisterende.oppdaterOppgave(this)
                deltakelseRepository.save(eksisterende)
            }

        logger.info("Oppretter ny oppgave for bekreftelse av endret startdato")
        val nyOppgave = OppgaveDAO(
            id = UUID.randomUUID(),
            deltakelse = eksisterende,
            oppgavetype = Oppgavetype.BEKREFT_ENDRET_STARTDATO,
            oppgavetypeDataDAO = EndretStartdatoOppgavetypeDataDAO(
                nyStartdato = startdato,
                veilederRef = endrePeriodeDatoDTO.veilederRef,
                meldingFraVeileder = endrePeriodeDatoDTO.meldingFraVeileder
            ),
            status = OppgaveStatus.ULØST,
            opprettetDato = ZonedDateTime.now(ZoneOffset.UTC),
            løstDato = null
        )

        val nyPeriodeMedEndretStartdato: Range<LocalDate> = if (sluttdato != null) {
            Range.closed(startdato, sluttdato)
        } else {
            Range.closedInfinite(startdato)
        }

        eksisterende.oppdaterPeriode(nyPeriodeMedEndretStartdato)
        eksisterende.leggTilOppgave(nyOppgave)
        val oppdatertDeltakelse = deltakelseRepository.save(eksisterende)

        sendEndretStartdatoHendelseTilUngSak(oppdatertDeltakelse)

        return oppdatertDeltakelse.mapToDTO()
    }

    fun endreSluttdato(deltakelseId: UUID, endrePeriodeDatoDTO: EndrePeriodeDatoDTO): DeltakelseOpplysningDTO {
        val eksisterende = forsikreEksistererIProgram(deltakelseId)
        logger.info("Endrer sluttdato for deltakelse med id $deltakelseId fra ${eksisterende.getTom()} til $endrePeriodeDatoDTO")

        val startdato = eksisterende.getFom()
        val sluttdato = endrePeriodeDatoDTO.dato
        forsikreGyldigPeriode(sluttdato, startdato)

        eksisterende.oppgaver.find { it.oppgavetype == Oppgavetype.BEKREFT_ENDRET_SLUTTDATO && it.status == OppgaveStatus.ULØST }
            ?.apply {
                logger.info("Fant uløst oppgave for endring av sluttdato. Markerer som kansellert.")
                markerSomKansellert()
                eksisterende.oppdaterOppgave(this)
                deltakelseRepository.save(eksisterende)
            }

        val bekreftEndretSluttdatoOppgave = OppgaveDAO(
            id = UUID.randomUUID(),
            oppgavetype = Oppgavetype.BEKREFT_ENDRET_SLUTTDATO,
            oppgavetypeDataDAO = EndretSluttdatoOppgavetypeDataDAO(
                nySluttdato = endrePeriodeDatoDTO.dato,
                veilederRef = endrePeriodeDatoDTO.veilederRef,
                meldingFraVeileder = endrePeriodeDatoDTO.meldingFraVeileder
            ),
            status = OppgaveStatus.ULØST,
            deltakelse = eksisterende
        )

        val nyPeriodeMedEndretSluttdato = Range.closed(eksisterende.getFom(), endrePeriodeDatoDTO.dato)

        eksisterende.oppdaterPeriode(nyPeriodeMedEndretSluttdato)
        eksisterende.leggTilOppgave(bekreftEndretSluttdatoOppgave)
        val oppdatertDeltakelse = deltakelseRepository.save(eksisterende)

        sendEndretSluttdatoHendelseTilUngSak(oppdatertDeltakelse)

        return oppdatertDeltakelse.mapToDTO()
    }

    fun hentOppgaveForDeltakelse(personIdent: String, deltakelseId: UUID, oppgaveId: UUID): OppgaveDTO {
        logger.info("Henter oppgave med id $oppgaveId for deltakelse med id $deltakelseId")
        val deltakerIder = deltakerService.hentDeltakterIder(personIdent)
        val deltakelse =
            deltakelseRepository.findByIdAndDeltaker_IdIn(deltakelseId, deltakerIder) ?: throw ErrorResponseException(
                HttpStatus.NOT_FOUND,
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND).also {
                    it.detail = "Fant ingen deltakelse med id $deltakelseId"
                },
                null
            )

        val oppgave = deltakelse.oppgaver.find { oppgave -> oppgave.id == oppgaveId } ?: run {
            throw ErrorResponseException(
                HttpStatus.NOT_FOUND,
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND).also {
                    it.detail = "Fant ingen oppgave med id $oppgaveId for deltakelse med id $deltakelseId"
                },
                null
            )
        }

        return oppgave.tilDTO()
    }

    private fun sendEndretSluttdatoHendelseTilUngSak(oppdatert: UngdomsprogramDeltakelseDAO) {
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

    private fun sendEndretStartdatoHendelseTilUngSak(oppdatert: UngdomsprogramDeltakelseDAO) {
        val startdato = oppdatert.getFom()

        logger.info("Henter aktørIder for deltaker")
        val aktørIder = pdlService.hentAktørIder(oppdatert.deltaker.deltakerIdent, historisk = true)
        val nåværendeAktørId = aktørIder.first { !it.historisk }.ident

        logger.info("Sender inn hendelse til ung-sak om at programmet har endret startdato")

        val hendelsedato =
            oppdatert.endretTidspunkt?.toLocalDateTime() ?: oppdatert.opprettetTidspunkt.toLocalDateTime()
        val hendelseInfo = HendelseInfo.Builder().medOpprettet(hendelsedato)
        aktørIder.forEach {
            hendelseInfo.leggTilAktør(AktørId(it.ident))
        }

        val hendelse = UngdomsprogramEndretStartdatoHendelse(hendelseInfo.build(), startdato)
        ungSakService.sendInnHendelse(hendelse = HendelseDto(hendelse, AktørId(nåværendeAktørId)))
    }

    private fun UngdomsprogramDeltakelseDAO.mapToDTO(): DeltakelseOpplysningDTO {

        return DeltakelseOpplysningDTO(
            id = id,
            deltaker = deltaker.mapToDTO(),
            harSøkt = harSøkt,
            fraOgMed = getFom(),
            tilOgMed = getTom(),
            oppgaver = oppgaver.map { it.tilDTO() }
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

    private fun forsikreGyldigPeriode(sluttdato: LocalDate?, startdato: LocalDate) {
        if (sluttdato != null && sluttdato < startdato) {
            throw ErrorResponseException(
                HttpStatus.BAD_REQUEST,
                ProblemDetail.forStatus(HttpStatus.BAD_REQUEST).also {
                    it.detail = "Ny startdato kan ikke være etter sluttdato"
                },
                null
            )
        }
    }
}
