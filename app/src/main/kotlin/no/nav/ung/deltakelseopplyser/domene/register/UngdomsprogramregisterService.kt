package no.nav.ung.deltakelseopplyser.domene.register

import io.hypersistence.utils.hibernate.type.range.Range
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.ung.deltakelseopplyser.config.DeltakerappConfig
import no.nav.ung.deltakelseopplyser.config.TxConfiguration.Companion.TRANSACTION_MANAGER
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDAO
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService.Companion.mapToDTO
import no.nav.ung.deltakelseopplyser.domene.inntekt.RapportertInntektService
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveDAO.Companion.tilDTO
import no.nav.ung.deltakelseopplyser.domene.minside.MineSiderService
import no.nav.ung.deltakelseopplyser.integration.pdl.api.PdlService
import no.nav.ung.deltakelseopplyser.integration.ungsak.UngSakService
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakelsePeriodInfo
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveStatus
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseHistorikkDTO
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseOpplysningDTO
import no.nav.ung.deltakelseopplyser.kontrakt.register.Revisjonstype
import no.nav.ung.deltakelseopplyser.kontrakt.veileder.EndrePeriodeDatoDTO
import no.nav.ung.sak.kontrakt.hendelser.HendelseDto
import no.nav.ung.sak.kontrakt.hendelser.HendelseInfo
import no.nav.ung.sak.kontrakt.hendelser.UngdomsprogramEndretStartdatoHendelse
import no.nav.ung.sak.kontrakt.hendelser.UngdomsprogramOpphørHendelse
import no.nav.ung.sak.typer.AktørId
import org.slf4j.LoggerFactory
import org.springframework.data.history.Revision
import org.springframework.data.history.RevisionMetadata
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.ErrorResponseException
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

@Service
class UngdomsprogramregisterService(
    private val deltakelseRepository: UngdomsprogramDeltakelseRepository,
    private val deltakerService: DeltakerService,
    private val ungSakService: UngSakService,
    private val pdlService: PdlService,
    private val rapportertInntektService: RapportertInntektService,
    private val mineSiderService: MineSiderService,
    private val deltakerappConfig: DeltakerappConfig,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(UngdomsprogramregisterService::class.java)

        fun UngdomsprogramDeltakelseDAO.mapToDTO(): DeltakelseOpplysningDTO {

            return DeltakelseOpplysningDTO(
                id = id,
                deltaker = deltaker.mapToDTO(),
                søktTidspunkt = søktTidspunkt,
                fraOgMed = getFom(),
                tilOgMed = getTom(),
                oppgaver = deltaker.oppgaver.map { it.tilDTO() }
            )
        }
    }

    fun deltakelseHistorikk(id: UUID): List<DeltakelseHistorikkDTO> {
        logger.info("Henter historikk for deltakelse med id $id")
        return deltakelseRepository.findRevisions(id).stream()
            .map { revision: Revision<Long, UngdomsprogramDeltakelseDAO> ->
                val metadata = revision.metadata

                val deltakelseDAO = revision.entity
                DeltakelseHistorikkDTO(
                    revisjonstype = metadata.revisionType.somHistorikkType(),
                    revisjonsnummer = metadata.revisionNumber.get(),
                    id = deltakelseDAO.id,
                    fom = deltakelseDAO.getFom(),
                    tom = deltakelseDAO.getTom(),
                    opprettetAv = deltakelseDAO.opprettetAv,
                    opprettetTidspunkt = deltakelseDAO.opprettetTidspunkt.atZone(ZoneOffset.UTC),
                    endretAv = deltakelseDAO.endretAv!!,
                    endretTidspunkt = deltakelseDAO.endretTidspunkt!!.atZone(ZoneOffset.UTC),
                    søktTidspunkt = deltakelseDAO.søktTidspunkt,
                )
            }
            .toList()
            .also {
                logger.info("Fant ${it.size} historikkoppføringer for deltakelse med id $id")
            }
    }

    @Transactional(TRANSACTION_MANAGER)
    fun leggTilIProgram(deltakelseOpplysningDTO: DeltakelseOpplysningDTO): DeltakelseOpplysningDTO {
        logger.info("Legger til deltaker i programmet: $deltakelseOpplysningDTO")

        val deltakerDAO = deltakerService.finnDeltakerGittIdent(deltakelseOpplysningDTO.deltaker.deltakerIdent) ?: run {
            logger.info("Deltaker eksisterer ikke. Oppretter ny deltaker.")
            deltakerService.lagreDeltaker(deltakelseOpplysningDTO)
        }

        val ungdomsprogramDAO = deltakelseRepository.save(deltakelseOpplysningDTO.mapToDAO(deltakerDAO))

        varsleDeltakerOmInnmelding(ungdomsprogramDAO, deltakerDAO)

        return ungdomsprogramDAO.mapToDTO()
    }

    private fun varsleDeltakerOmInnmelding(
        ungdomsprogramDAO: UngdomsprogramDeltakelseDAO,
        deltakerDAO: DeltakerDAO,
    ) {
        mineSiderService.opprettVarsel(
            varselId = ungdomsprogramDAO.id.toString(),
            deltakerIdent = deltakerDAO.deltakerIdent,
            tekster = listOf(
                Tekst(
                    tekst = "Du er registrert i ungdomsprogrammet. Klikk her for å søke.",
                    spraakkode = "nb",
                    default = true
                )
            ),
            varselLink = deltakerappConfig.getSøknadUrl(),
            varseltype = Varseltype.Beskjed,
            aktivFremTil = ZonedDateTime.now().plusMonths(3)
        )
    }

    @Transactional(TRANSACTION_MANAGER)
    fun fjernFraProgram(deltakerId: UUID): Boolean {
        logger.info("Fjerner deltaker fra programmet med id $deltakerId")

        val deltaker = deltakerService.finnDeltakerGittId(deltakerId)
        if (!deltaker.isPresent) {
            logger.info("Deltaker med id $deltakerId eksisterer ikke. Returnerer true")
            return true
        }

        val deltakelser = hentAlleForDeltakerId(deltakerId)
        val harSøkteDeltakelser = deltakelser.any { it.søktTidspunkt != null }

        if (harSøkteDeltakelser) {
            logger.error("Klarte ikke å slette deltaker fra programmet med id $deltakerId, fordi deltakeren har søkt")
            throw ErrorResponseException(
                HttpStatus.FORBIDDEN,
                ProblemDetail.forStatus(HttpStatus.FORBIDDEN).also {
                    it.detail = "Deltakeren har søkt og deltakelsen kan derfor ikke slettes"
                },
                null
            )
        }

        val deltakerSlettet = deltakerService.slettDeltaker(deltakerId)
        if (!deltakerSlettet) {
            logger.error("Klarte ikke å slette deltaker med id $deltakerId fra deltakerregisteret")
            throw ErrorResponseException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR).also {
                    it.detail = "Klarte ikke å slette deltaker fra deltakerregisteret"
                },
                null
            )
        }

        val detFinnesDeltakelser = deltakelseRepository.findByDeltaker_IdIn(listOf(deltakerId)).isNotEmpty()
        if (detFinnesDeltakelser) {
            logger.error("Klarte ikke å slette deltakelser registrert på deltaker med id $deltakerId")
            throw ErrorResponseException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR).also {
                    it.detail = "Det finnes fortsatt deltakelser registrert på deltaker med id $deltakerId"
                },
                null
            )
        }

        return true
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
        val deltakersOppgaver = deltakerService.hentDeltakersOppgaver(deltakerIdentEllerAktørId)
        val ungdomsprogramDAOs = deltakelseRepository.findByDeltaker_IdIn(deltakterIder)
        logger.info("Fant ${ungdomsprogramDAOs.size} programopplysninger for deltaker.")

        return ungdomsprogramDAOs.map { it.tilDeltakelsePeriodInfo(deltakersOppgaver) }
    }

    @Transactional(TRANSACTION_MANAGER)
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

    @Transactional(TRANSACTION_MANAGER)
    fun endreStartdato(deltakelseId: UUID, endrePeriodeDatoDTO: EndrePeriodeDatoDTO): DeltakelseOpplysningDTO {
        val eksisterende = forsikreEksistererIProgram(deltakelseId)
        logger.info("Endrer startdato for deltakelse med id $deltakelseId fra ${eksisterende.getFom()} til $endrePeriodeDatoDTO")

        val startdato = endrePeriodeDatoDTO.dato
        val sluttdato = eksisterende.getTom()
        forsikreGyldigPeriode(sluttdato, startdato)

        val nyPeriodeMedEndretStartdato: Range<LocalDate> = if (sluttdato != null) {
            Range.closed(startdato, sluttdato)
        } else {
            Range.closedInfinite(startdato)
        }
        eksisterende.oppdaterPeriode(nyPeriodeMedEndretStartdato)
        val oppdatertDeltakelse = deltakelseRepository.save(eksisterende)

        sendEndretStartdatoHendelseTilUngSak(oppdatertDeltakelse)

        return oppdatertDeltakelse.mapToDTO()
    }

    @Transactional(TRANSACTION_MANAGER)
    fun endreSluttdato(deltakelseId: UUID, endrePeriodeDatoDTO: EndrePeriodeDatoDTO): DeltakelseOpplysningDTO {
        val eksisterende = forsikreEksistererIProgram(deltakelseId)
        logger.info("Endrer sluttdato for deltakelse med id $deltakelseId fra ${eksisterende.getTom()} til $endrePeriodeDatoDTO")

        val startdato = eksisterende.getFom()
        val sluttdato = endrePeriodeDatoDTO.dato
        forsikreGyldigPeriode(sluttdato, startdato)

        val nyPeriodeMedEndretSluttdato = Range.closed(eksisterende.getFom(), endrePeriodeDatoDTO.dato)
        eksisterende.oppdaterPeriode(nyPeriodeMedEndretSluttdato)
        val oppdatertDeltakelse = deltakelseRepository.save(eksisterende)

        sendEndretSluttdatoHendelseTilUngSak(oppdatertDeltakelse)

        return oppdatertDeltakelse.mapToDTO()
    }

    private fun sendEndretSluttdatoHendelseTilUngSak(oppdatert: UngdomsprogramDeltakelseDAO) {
        val opphørsdato = oppdatert.getTom()
        requireNotNull(opphørsdato) { "Til og med dato må være satt for å sende inn hendelse til ung-sak" }

        logger.info("Henter aktørIder for deltaker")
        val aktørIder = pdlService.hentAktørIder(oppdatert.deltaker.deltakerIdent, historisk = true)
        val nåværendeAktørId = aktørIder.first { !it.historisk }.ident

        logger.info("Sender inn hendelse til ung-sak om at deltaker har opphørt programmet")

        val hendelsedato =
            oppdatert.endretTidspunkt?.atZone(ZoneOffset.UTC)?.toLocalDateTime()
                ?: oppdatert.opprettetTidspunkt.atZone(ZoneOffset.UTC).toLocalDateTime()

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
            oppdatert.endretTidspunkt?.atZone(ZoneOffset.UTC)?.toLocalDateTime()
                ?: oppdatert.opprettetTidspunkt.atZone(ZoneOffset.UTC).toLocalDateTime()

        val hendelseInfo = HendelseInfo.Builder().medOpprettet(hendelsedato)
        aktørIder.forEach {
            hendelseInfo.leggTilAktør(AktørId(it.ident))
        }

        val hendelse = UngdomsprogramEndretStartdatoHendelse(hendelseInfo.build(), startdato)
        ungSakService.sendInnHendelse(hendelse = HendelseDto(hendelse, AktørId(nåværendeAktørId)))
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
            søktTidspunkt = søktTidspunkt
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

    private fun UngdomsprogramDeltakelseDAO.tilDeltakelsePeriodInfo(oppgaver: List<OppgaveDAO>): DeltakelsePeriodInfo {
        val oppgaver = oppgaver.map { oppgave ->
            oppgave
                .tilDTO()
                .let { oppgaveDTO ->
                    if (oppgave.erLøstInntektsrapportering()) {
                        rapportertInntektService.leggPåRapportertInntekt(oppgaveDTO)
                    } else oppgaveDTO
                }
        }

        return DeltakelsePeriodInfo(
            id = this.id,
            fraOgMed = getFom(),
            tilOgMed = getTom(),
            søktTidspunkt = this.søktTidspunkt,
            oppgaver = oppgaver
        )
    }

    private fun OppgaveDAO.erLøstInntektsrapportering() =
        this.oppgavetype == Oppgavetype.RAPPORTER_INNTEKT && this.status == OppgaveStatus.LØST

    private fun RevisionMetadata.RevisionType.somHistorikkType() = when (this) {
        RevisionMetadata.RevisionType.INSERT -> Revisjonstype.OPPRETTET
        RevisionMetadata.RevisionType.UPDATE -> Revisjonstype.ENDRET
        RevisionMetadata.RevisionType.DELETE -> Revisjonstype.SLETTET
        RevisionMetadata.RevisionType.UNKNOWN -> Revisjonstype.UKJENT
    }
}
