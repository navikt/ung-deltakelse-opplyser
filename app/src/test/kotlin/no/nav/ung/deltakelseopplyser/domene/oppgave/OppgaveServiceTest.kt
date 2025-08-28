package no.nav.ung.deltakelseopplyser.domene.oppgave

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import no.nav.k9.oppgave.OppgaveBekreftelse
import no.nav.k9.oppgave.bekreftelse.Bekreftelse
import no.nav.k9.oppgave.bekreftelse.ung.inntekt.InntektBekreftelse
import no.nav.k9.oppgave.bekreftelse.ung.periodeendring.EndretSluttdatoBekreftelse
import no.nav.k9.oppgave.bekreftelse.ung.periodeendring.EndretStartdatoBekreftelse
import no.nav.k9.søknad.felles.Kildesystem
import no.nav.k9.søknad.felles.Versjon
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.SøknadId
import no.nav.pdl.generated.enums.IdentGruppe
import no.nav.pdl.generated.hentident.IdentInformasjon
import no.nav.ung.deltakelseopplyser.AbstractIntegrationTest
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerRepository
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.minside.MineSiderService
import no.nav.ung.deltakelseopplyser.domene.oppgave.kafka.UngdomsytelseOppgavebekreftelse
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.EndretSluttdatoOppgaveDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.EndretStartdatoOppgaveDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.KontrollerRegisterInntektOppgaveTypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.SøkYtelseOppgavetypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseRepository
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService
import no.nav.ung.deltakelseopplyser.domene.register.ungsak.OppgaveUngSakController
import no.nav.ung.deltakelseopplyser.integration.abac.TilgangskontrollService
import no.nav.ung.deltakelseopplyser.integration.pdl.api.PdlService
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveStatus
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektArbeidOgFrilansDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektOppgaveDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektYtelseDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.YtelseType
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretSluttdatoOppgaveDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretStartdatoOppgaveDTO
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseDTO
import no.nav.ung.deltakelseopplyser.utils.FødselsnummerGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

@ActiveProfiles("test")
class OppgaveServiceTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var oppgaveService: OppgaveService

    @Autowired
    lateinit var deltakelseService: UngdomsprogramregisterService

    @Autowired
    lateinit var deltakelseRepository: DeltakelseRepository

    @Autowired
    lateinit var deltakerRepository: DeltakerRepository

    @Autowired
    lateinit var oppgaveUngSakController: OppgaveUngSakController

    @Autowired
    lateinit var deltakerService: DeltakerService

    @SpykBean
    lateinit var mineSiderService: MineSiderService

    @MockkBean
    lateinit var pdlService: PdlService

    @MockkBean
    lateinit var tilgangskontrollService: TilgangskontrollService

    private companion object {
        const val deltakerAktørId = "10987654321"

    }

    override val consumerGroupPrefix: String
        get() = "oppgave-service-test"
    override val consumerGroupTopics: List<String>
        get() = listOf()

    @BeforeEach
    fun setUpEach() {
        every { pdlService.hentAktørIder(any(), any()) } returns listOf(
            IdentInformasjon(
                ident = deltakerAktørId,
                historisk = false,
                gruppe = IdentGruppe.AKTORID
            )
        )


        justRun { tilgangskontrollService.krevSystemtilgang() }
    }

    @AfterEach
    fun verifiser() {
        verify(atLeast = 1, verifyBlock = {tilgangskontrollService.krevSystemtilgang()})
    }

    @Test
    fun `Gitt det mottas bekreftelse på endret startdato oppgave, forvent at den lagres og hentes opp igjen`() {
        val orginalStartdato: LocalDate = LocalDate.now()
        val deltakerIdent = FødselsnummerGenerator.neste()
        mockHentFolkeregisteridenter(deltakerIdent)

        meldInnIProgrammet(deltakerIdent, orginalStartdato)

        endreStartdato(
            deltakerIdent = deltakerIdent,
            originalStartdato = orginalStartdato,
            nyStartdato = orginalStartdato.plusDays(3)
        )

        val oppgaver = deltakerService.hentDeltakersOppgaver(deltakerIdent)
        assertThat(oppgaver)
            .hasSize(2)
            .anyMatch { it.oppgavetype == Oppgavetype.SØK_YTELSE }
            .anyMatch { it.oppgavetypeDataDAO is SøkYtelseOppgavetypeDataDAO }
            .anyMatch { it.oppgavetype == Oppgavetype.BEKREFT_ENDRET_STARTDATO }
            .anyMatch { it.oppgavetypeDataDAO is EndretStartdatoOppgaveDataDAO }

        val oppgaveReferanse =
            oppgaver.first { it.oppgavetype == Oppgavetype.BEKREFT_ENDRET_STARTDATO }.oppgaveReferanse

        oppgaveService.håndterMottattOppgavebekreftelse(
            oppgaveBekreftelse(
                oppgaveReferanse,
                deltakerIdent,
                EndretStartdatoBekreftelse(
                    oppgaveReferanse,
                    LocalDate.now(),
                    true
                ).medUttalelseFraBruker("Det er feil med datoen")
            )
        )

        val oppgave = deltakerService.hentDeltakersOppgaver(deltakerIdent)
            .first { it.oppgaveReferanse == oppgaveReferanse }

        assertThat(oppgave.status).isEqualTo(OppgaveStatus.LØST)
        assertThat(oppgave.oppgaveReferanse).isEqualTo(oppgaveReferanse)
        assertThat(oppgave.oppgavetypeDataDAO).isInstanceOf(EndretStartdatoOppgaveDataDAO::class.java)
        assertThat(oppgave.oppgaveBekreftelse).isNotNull
        assertThat(oppgave.oppgaveBekreftelse?.harUttalelse).isTrue()
        assertThat(oppgave.oppgaveBekreftelse?.uttalelseFraBruker).isEqualTo("Det er feil med datoen")

        verify(exactly = 1) { mineSiderService.deaktiverOppgave(oppgaveReferanse.toString()) }
    }


    @Test
    fun `Gitt det mottas bekreftelse på endret sluttdato oppgave, forvent at den lagres og hentes opp igjen`() {
        val startdato: LocalDate = LocalDate.now()
        val deltakerIdent = FødselsnummerGenerator.neste()
        mockHentFolkeregisteridenter(deltakerIdent)

        meldInnIProgrammet(deltakerIdent, startdato)

        endreSluttdato(
            deltakerIdent = deltakerIdent,
            nySluttdato = startdato.plusDays(3),
            originalSluttdato = null
        )

        val oppgaver = deltakerService.hentDeltakersOppgaver(deltakerIdent)
        assertThat(oppgaver)
            .hasSize(2)
            .anyMatch { it.oppgavetype == Oppgavetype.SØK_YTELSE }
            .anyMatch { it.oppgavetypeDataDAO is SøkYtelseOppgavetypeDataDAO }
            .anyMatch { it.oppgavetype == Oppgavetype.BEKREFT_ENDRET_SLUTTDATO }
            .anyMatch { it.oppgavetypeDataDAO is EndretSluttdatoOppgaveDataDAO }

        val oppgaveReferanse =
            oppgaver.first { it.oppgavetype == Oppgavetype.BEKREFT_ENDRET_SLUTTDATO }.oppgaveReferanse

        oppgaveService.håndterMottattOppgavebekreftelse(
            oppgaveBekreftelse(
                oppgaveReferanse,
                deltakerIdent,
                EndretSluttdatoBekreftelse(
                    oppgaveReferanse,
                    startdato.plusDays(3),
                    true
                ).medUttalelseFraBruker("Det er feil med datoen")
            )
        )


        val oppgave = deltakerService.hentDeltakersOppgaver(deltakerIdent)
            .first { it.oppgaveReferanse == oppgaveReferanse }

        assertThat(oppgave.status).isEqualTo(OppgaveStatus.LØST)
        assertThat(oppgave.oppgaveReferanse).isEqualTo(oppgaveReferanse)
        assertThat(oppgave.oppgavetypeDataDAO).isInstanceOf(EndretSluttdatoOppgaveDataDAO::class.java)
        assertThat(oppgave.oppgaveBekreftelse).isNotNull
        assertThat(oppgave.oppgaveBekreftelse?.harUttalelse).isTrue()
        assertThat(oppgave.oppgaveBekreftelse?.uttalelseFraBruker).isEqualTo("Det er feil med datoen")

        verify(exactly = 1) { mineSiderService.deaktiverOppgave(oppgaveReferanse.toString()) }
    }

    @Test
    fun `Gitt det mottas bekreftelse på avvik på registerinntekt oppgave, forvent at den lagres og hentes opp igjen`() {
        val orginalStartdato: LocalDate = LocalDate.now()
        val deltakerIdent = FødselsnummerGenerator.neste()
        mockHentFolkeregisteridenter(deltakerIdent)
        meldInnIProgrammet(deltakerIdent, orginalStartdato)

        kontrollerAvvikPåInntektIRegister(
            deltakerIdent = deltakerIdent,
            fom = orginalStartdato,
            tom = orginalStartdato.plusWeeks(4)
        )

        val oppgaver = deltakerService.hentDeltakersOppgaver(deltakerIdent)
        assertThat(oppgaver).hasSize(2)
        val oppgaveReferanse =
            oppgaver.first { it.oppgavetype == Oppgavetype.BEKREFT_AVVIK_REGISTERINNTEKT }.oppgaveReferanse

        oppgaveService.håndterMottattOppgavebekreftelse(
            oppgaveBekreftelse(
                oppgaveReferanse,
                deltakerIdent,
                InntektBekreftelse(
                    oppgaveReferanse,
                    true,
                    "Det er feil inntekt i registeret"
                )
            )
        )

        val oppgave = deltakerService.hentDeltakersOppgaver(deltakerIdent)
            .first { it.oppgaveReferanse == oppgaveReferanse }

        assertThat(oppgave.status).isEqualTo(OppgaveStatus.LØST)
        assertThat(oppgave.oppgaveReferanse).isEqualTo(oppgaveReferanse)
        assertThat(oppgave.oppgavetypeDataDAO).isInstanceOf(KontrollerRegisterInntektOppgaveTypeDataDAO::class.java)
        assertThat(oppgave.oppgaveBekreftelse).isNotNull
        assertThat(oppgave.oppgaveBekreftelse?.harUttalelse).isTrue()
        assertThat(oppgave.oppgaveBekreftelse?.uttalelseFraBruker).isEqualTo("Det er feil inntekt i registeret")

        verify(exactly = 1) { mineSiderService.deaktiverOppgave(oppgaveReferanse.toString()) }

    }


    @Test
    fun `Gitt det mottas feil type bekreftelse på oppgave, forvent at kastes feil`() {
        val originalStartdato: LocalDate = LocalDate.now()
        val deltakerIdent = FødselsnummerGenerator.neste()
        mockHentFolkeregisteridenter(deltakerIdent)
        meldInnIProgrammet(deltakerIdent, originalStartdato)

        endreStartdato(
            deltakerIdent = deltakerIdent,
            originalStartdato = originalStartdato,
            nyStartdato = originalStartdato.plusDays(3)
        )

        val oppgaver = deltakerService.hentDeltakersOppgaver(deltakerIdent)
        assertThat(oppgaver).hasSize(2)
        val oppgaveReferanse =
            oppgaver.first { it.oppgavetype == Oppgavetype.BEKREFT_ENDRET_STARTDATO }.oppgaveReferanse

        assertThrows<IllegalStateException> {
            oppgaveService.håndterMottattOppgavebekreftelse(
                oppgaveBekreftelse(
                    oppgaveReferanse,
                    deltakerIdent,
                    InntektBekreftelse(
                        oppgaveReferanse,
                        true,
                        "Det er feil inntekt"
                    )
                )
            )
        }

        verify(exactly = 0) { mineSiderService.deaktiverOppgave(oppgaveReferanse.toString()) }
    }

    private fun mockHentFolkeregisteridenter(deltakerIdent: String) {
        every { pdlService.hentFolkeregisteridenter(any()) } returns listOf(
            IdentInformasjon(
                ident = deltakerIdent,
                historisk = false,
                gruppe = IdentGruppe.FOLKEREGISTERIDENT
            )
        )
    }

    private fun endreStartdato(
        deltakerIdent: String,
        originalStartdato: LocalDate,
        nyStartdato: LocalDate,
    ) {
        oppgaveUngSakController.opprettOppgaveForEndretStartdato(
            endretStartdatoOppgaveDTO = EndretStartdatoOppgaveDTO(
                deltakerIdent = deltakerIdent,
                oppgaveReferanse = UUID.randomUUID(),
                frist = LocalDateTime.now().plusDays(14),
                nyStartdato = nyStartdato,
                forrigeStartdato = originalStartdato,
            )
        )
    }

    private fun endreSluttdato(
        deltakerIdent: String,
        nySluttdato: LocalDate,
        originalSluttdato: LocalDate?,
    ) {
        oppgaveUngSakController.opprettOppgaveForEndretSluttdato(
            endretSluttdatoOppgaveDTO = EndretSluttdatoOppgaveDTO(
                deltakerIdent = deltakerIdent,
                oppgaveReferanse = UUID.randomUUID(),
                frist = LocalDateTime.now().plusDays(14),
                nySluttdato = nySluttdato,
                forrigeSluttdato = originalSluttdato,
            )
        )
    }


    private fun kontrollerAvvikPåInntektIRegister(
        deltakerIdent: String,
        fom: LocalDate,
        tom: LocalDate,
    ) {
        oppgaveUngSakController.opprettOppgaveForKontrollAvRegisterinntekt(
            opprettOppgaveDto = RegisterInntektOppgaveDTO(
                deltakerIdent = deltakerIdent,
                referanse = UUID.randomUUID(),
                frist = LocalDateTime.now().plusDays(14),
                fomDato = fom,
                tomDato = tom,
                registerInntekter = RegisterInntektDTO(
                    registerinntekterForArbeidOgFrilans = listOf(
                        RegisterInntektArbeidOgFrilansDTO(1000, "123"),
                        RegisterInntektArbeidOgFrilansDTO(2000, "321"),
                    ),
                    registerinntekterForYtelse = listOf(
                        RegisterInntektYtelseDTO(1000, YtelseType.SYKEPENGER),
                        RegisterInntektYtelseDTO(2000, YtelseType.PLEIEPENGER_SYKT_BARN),
                    )
                )
            )
        )
    }

    fun oppgaveBekreftelse(
        oppgaveReferanse: UUID,
        deltakerIdent: String,
        bekreftelse: Bekreftelse,
    ): UngdomsytelseOppgavebekreftelse = UngdomsytelseOppgavebekreftelse(
        oppgaveBekreftelse = OppgaveBekreftelse()
            .medSøknadId(SøknadId(oppgaveReferanse.toString()))
            .medVersjon(Versjon("1.0.0"))
            .medMottattDato(ZonedDateTime.now())
            .medKildesystem(Kildesystem.SØKNADSDIALOG)
            .medSøker(Søker(NorskIdentitetsnummer.of(deltakerIdent)))
            .medBekreftelse(bekreftelse),
        journalpostId = "123456",
    )


    private fun meldInnIProgrammet(søkerIdent: String, deltakelseStart: LocalDate): DeltakelseDTO {
        return deltakelseService.leggTilIProgram(
            deltakelseDTO = DeltakelseDTO(
                deltaker = DeltakerDTO(deltakerIdent = søkerIdent),
                fraOgMed = deltakelseStart,
                tilOgMed = null
            )
        )
    }
}
