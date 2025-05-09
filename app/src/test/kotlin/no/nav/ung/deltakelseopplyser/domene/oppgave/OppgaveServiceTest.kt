package no.nav.ung.deltakelseopplyser.domene.oppgave

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import io.mockk.verify
import no.nav.k9.oppgave.OppgaveBekreftelse
import no.nav.k9.oppgave.bekreftelse.Bekreftelse
import no.nav.k9.oppgave.bekreftelse.ung.inntekt.InntektBekreftelse
import no.nav.k9.oppgave.bekreftelse.ung.periodeendring.EndretProgramperiodeBekreftelse
import no.nav.k9.søknad.felles.Kildesystem
import no.nav.k9.søknad.felles.Versjon
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.felles.type.SøknadId
import no.nav.pdl.generated.enums.IdentGruppe
import no.nav.pdl.generated.hentident.IdentInformasjon
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.ung.deltakelseopplyser.AbstractIntegrationTest
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerRepository
import no.nav.ung.deltakelseopplyser.domene.oppgave.kafka.UngdomsytelseOppgavebekreftelse
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramDeltakelseRepository
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService
import no.nav.ung.deltakelseopplyser.domene.register.ungsak.OppgaveUngSakController
import no.nav.ung.deltakelseopplyser.domene.varsler.MineSiderVarselService
import no.nav.ung.deltakelseopplyser.integration.pdl.api.PdlService
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.EndretProgramperiodeDataDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.KontrollerRegisterinntektOppgavetypeDataDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveStatus
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.periodeendring.EndretProgamperiodeOppgaveDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.periodeendring.ProgramperiodeDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektArbeidOgFrilansDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektOppgaveDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektYtelseDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.YtelseType
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseOpplysningDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

@EnableMockOAuth2Server
@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OppgaveServiceTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var oppgaveService: OppgaveService

    @Autowired
    lateinit var deltakelseService: UngdomsprogramregisterService

    @Autowired
    lateinit var deltakelseRepository: UngdomsprogramDeltakelseRepository

    @Autowired
    lateinit var deltakerRepository: DeltakerRepository

    @Autowired
    lateinit var oppgaveUngSakController: OppgaveUngSakController

    @SpykBean
    lateinit var mineSiderVarselService: MineSiderVarselService

    @MockkBean
    lateinit var pdlService: PdlService

    private companion object {
        const val deltakerIdent = "12345678901"
        const val deltakerAktørId = "10987654321"

    }

    override val consumerGroupPrefix: String
        get() = "oppgave-service-test"
    override val consumerGroupTopics: List<String>
        get() = listOf()

    @BeforeEach
    fun setUpEach() {
        deltakelseRepository.deleteAll()
        deltakerRepository.deleteAll()

        every { pdlService.hentAktørIder(any(), any()) } returns listOf(
            IdentInformasjon(
                ident = deltakerAktørId,
                historisk = false,
                gruppe = IdentGruppe.AKTORID
            )
        )

        every { pdlService.hentFolkeregisteridenter(any()) } returns listOf(
            IdentInformasjon(
                ident = deltakerIdent,
                historisk = false,
                gruppe = IdentGruppe.FOLKEREGISTERIDENT
            )
        )
    }

    @Test
    fun `Gitt det mottas bekreftelse på endret periode oppgave, forvent at den lagres og hentes opp igjen`() {
        val orginalStartdato: LocalDate = LocalDate.now()
        meldInnIProgrammet(deltakerIdent, orginalStartdato)

        endreProgramperiode(
            deltakerIdent = deltakerIdent,
            originalPeriode = ProgramperiodeDTO(
                fomDato = orginalStartdato,
                tomDato = null
            ),
            nyPeriode = ProgramperiodeDTO(
                fomDato = orginalStartdato.plusDays(3),
                tomDato = null
            )
        )

        val oppgaver = deltakelseService.hentAlleForDeltaker(deltakerIdent).first().oppgaver
        assertThat(oppgaver).hasSize(1)
        val oppgaveReferanse = oppgaver.first().oppgaveReferanse

        oppgaveService.håndterMottattOppgavebekreftelse(
            oppgaveBekreftelse(
                oppgaveReferanse,
                deltakerIdent,
                EndretProgramperiodeBekreftelse(
                    oppgaveReferanse,
                    Periode(LocalDate.now(), LocalDate.now().plusDays(30)),
                    false
                ).medUttalelseFraBruker("Det er feil med datoene")
            )
        )

        val oppdatertDeltakelse = deltakelseService.hentAlleForDeltaker(deltakerIdent).first()
        val oppgave = oppdatertDeltakelse.oppgaver.first()
        assertThat(oppgave.status).isEqualTo(OppgaveStatus.LØST)
        assertThat(oppgave.oppgaveReferanse).isEqualTo(oppgaveReferanse)
        assertThat(oppgave.oppgavetypeData).isInstanceOf(EndretProgramperiodeDataDTO::class.java)
        assertThat(oppgave.bekreftelse).isNotNull
        assertThat(oppgave.bekreftelse?.harGodtattEndringen).isFalse()
        assertThat(oppgave.bekreftelse?.uttalelseFraBruker).isEqualTo("Det er feil med datoene")

        verify(exactly = 1) { mineSiderVarselService.deaktiverOppgave(oppgaveReferanse.toString()) }
    }

    @Test
    fun `Gitt det mottas bekreftelse på avvik på registerinntekt oppgave, forvent at den lagres og hentes opp igjen`() {
        val orginalStartdato: LocalDate = LocalDate.now()
        meldInnIProgrammet(deltakerIdent, orginalStartdato)

        kontrollerAvvikPåInntektIRegister(
            deltakerIdent = deltakerIdent,
            periode = ProgramperiodeDTO(
                fomDato = orginalStartdato,
                tomDato = orginalStartdato.plusWeeks(4)
            )
        )

        val oppgaver = deltakelseService.hentAlleForDeltaker(deltakerIdent).first().oppgaver
        assertThat(oppgaver).hasSize(1)
        val oppgaveReferanse = oppgaver.first().oppgaveReferanse

        oppgaveService.håndterMottattOppgavebekreftelse(
            oppgaveBekreftelse(
                oppgaveReferanse,
                deltakerIdent,
                InntektBekreftelse(
                    oppgaveReferanse,
                    false,
                    "Det er feil inntekt i registeret"
                )
            )
        )

        val oppdatertDeltakelse = deltakelseService.hentAlleForDeltaker(deltakerIdent).first()
        val oppgave = oppdatertDeltakelse.oppgaver.first()
        assertThat(oppgave.status).isEqualTo(OppgaveStatus.LØST)
        assertThat(oppgave.oppgaveReferanse).isEqualTo(oppgaveReferanse)
        assertThat(oppgave.oppgavetypeData).isInstanceOf(KontrollerRegisterinntektOppgavetypeDataDTO::class.java)
        assertThat(oppgave.bekreftelse).isNotNull
        assertThat(oppgave.bekreftelse?.harGodtattEndringen).isFalse()
        assertThat(oppgave.bekreftelse?.uttalelseFraBruker).isEqualTo("Det er feil inntekt i registeret")

        verify(exactly = 1) { mineSiderVarselService.deaktiverOppgave(oppgaveReferanse.toString()) }

    }

    @Test
    fun `Gitt det mottas feil type bekreftelse på oppgave, forvent at kastes feil`() {
        val orginalStartdato: LocalDate = LocalDate.now()
        meldInnIProgrammet(deltakerIdent, orginalStartdato)

        endreProgramperiode(
            deltakerIdent = deltakerIdent,
            originalPeriode = ProgramperiodeDTO(
                fomDato = orginalStartdato,
                tomDato = null
            ),
            nyPeriode = ProgramperiodeDTO(
                fomDato = orginalStartdato.plusDays(3),
                tomDato = null
            )
        )

        val oppgaver = deltakelseService.hentAlleForDeltaker(deltakerIdent).first().oppgaver
        assertThat(oppgaver).hasSize(1)
        val oppgaveReferanse = oppgaver.first().oppgaveReferanse

        assertThrows<IllegalStateException> {
            oppgaveService.håndterMottattOppgavebekreftelse(
                oppgaveBekreftelse(
                    oppgaveReferanse,
                    deltakerIdent,
                    InntektBekreftelse(
                        oppgaveReferanse,
                        false,
                        "Det er feil inntekt"
                    )
                )
            )
        }

        verify(exactly = 0) { mineSiderVarselService.deaktiverOppgave(oppgaveReferanse.toString()) }
    }

    private fun endreProgramperiode(
        deltakerIdent: String,
        originalPeriode: ProgramperiodeDTO,
        nyPeriode: ProgramperiodeDTO,
    ) {
        oppgaveUngSakController.opprettOppgaveForEndretProgramperiode(
            endretProgramperiodeOppgaveDTO = EndretProgamperiodeOppgaveDTO(
                deltakerIdent = deltakerIdent,
                oppgaveReferanse = UUID.randomUUID(),
                frist = LocalDateTime.now().plusDays(14),
                programperiode = nyPeriode,
                forrigeProgramperiode = originalPeriode,
            )
        )
    }

    private fun kontrollerAvvikPåInntektIRegister(
        deltakerIdent: String,
        periode: ProgramperiodeDTO,
    ) {
        oppgaveUngSakController.opprettOppgaveForKontrollAvRegisterinntekt(
            opprettOppgaveDto = RegisterInntektOppgaveDTO(
                deltakerIdent = deltakerIdent,
                referanse = UUID.randomUUID(),
                frist = LocalDateTime.now().plusDays(14),
                fomDato = periode.fomDato,
                tomDato = periode.tomDato!!,
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


    private fun meldInnIProgrammet(søkerIdent: String, deltakelseStart: LocalDate): DeltakelseOpplysningDTO {
        return deltakelseService.leggTilIProgram(
            deltakelseOpplysningDTO = DeltakelseOpplysningDTO(
                deltaker = DeltakerDTO(deltakerIdent = søkerIdent),
                harSøkt = false,
                fraOgMed = deltakelseStart,
                tilOgMed = null,
                oppgaver = listOf()
            )
        )
    }
}
