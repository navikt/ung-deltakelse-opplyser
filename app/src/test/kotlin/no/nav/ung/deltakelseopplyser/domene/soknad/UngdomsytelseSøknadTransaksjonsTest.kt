package no.nav.ung.deltakelseopplyser.domene.soknad

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.prosessering.internal.TaskWorker
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Kildesystem
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Språk
import no.nav.k9.søknad.ytelse.ung.v1.UngSøknadstype
import no.nav.k9.søknad.ytelse.ung.v1.Ungdomsytelse
import no.nav.pdl.generated.enums.IdentGruppe
import no.nav.pdl.generated.hentident.IdentInformasjon
import no.nav.tms.microfrontend.Sensitivitet
import no.nav.ung.deltakelseopplyser.AbstractIntegrationTest
import no.nav.ung.deltakelseopplyser.domene.deltaker.Scenarioer
import no.nav.ung.deltakelseopplyser.domene.minside.MineSiderService
import no.nav.ung.deltakelseopplyser.domene.minside.mikrofrontend.MicrofrontendId
import no.nav.ung.deltakelseopplyser.domene.minside.mikrofrontend.MicrofrontendRepository
import no.nav.ung.deltakelseopplyser.domene.minside.task.AktiverMikrofrontendMinSideTask
import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseRepository
import no.nav.ung.deltakelseopplyser.domene.register.KvotePeriodeBeregner
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramregisterService
import no.nav.ung.deltakelseopplyser.domene.soknad.kafka.Ungdomsytelsesøknad
import no.nav.ung.deltakelseopplyser.domene.soknad.repository.SøknadRepository
import no.nav.ung.deltakelseopplyser.integration.abac.SifAbacPdpService
import no.nav.ung.deltakelseopplyser.integration.enhetsregisteret.EnhetsregisterService
import no.nav.ung.deltakelseopplyser.integration.kontoregister.KontoregisterService
import no.nav.ung.deltakelseopplyser.integration.nom.api.NomApiService
import no.nav.ung.deltakelseopplyser.integration.pdl.api.PdlService
import no.nav.ung.deltakelseopplyser.integration.ungsak.UngBrukerdialogService
import no.nav.ung.deltakelseopplyser.integration.ungsak.UngSakService
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseDTO
import no.nav.ung.deltakelseopplyser.utils.FødselsnummerGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Transaksjonstester som kjører gjennom hele applikasjonsflyten:
 *
 *   UngdomsytelsesøknadService.håndterMottattSøknad
 *     -> MicrofrontendService.sendOgLagre oppretter AktiverMikrofrontendMinSideTask + mikrofrontend-rad
 *     -> TaskWorker kjører task'en som kaller MineSiderService.aktiverMikrofrontend (som i prod
 *        publiserer på min-side.aapen-microfrontend-v1)
 *
 * Hele første steget skjer i én og samme transaksjon (Kafka-lytteren UngdomsytelsesøknadKonsument er
 * @Transactional). Disse testene verifiserer transaksjonsgarantiene *slik de brukes av applikasjonen*,
 * i motsetning til å teste prosessering-core-biblioteket isolert.
 */
class UngdomsytelseSøknadTransaksjonsTest : AbstractIntegrationTest() {

    @MockkBean
    lateinit var pdlService: PdlService

    @MockkBean(relaxed = true)
    lateinit var sifAbacPdpService: SifAbacPdpService

    @MockkBean
    lateinit var ungSakService: UngSakService

    @MockkBean
    lateinit var ungBrukerdialogService: UngBrukerdialogService

    @MockkBean
    lateinit var kontoregisterService: KontoregisterService

    @MockkBean
    lateinit var enhetsregisterService: EnhetsregisterService

    @MockkBean(relaxed = true)
    lateinit var nomApiService: NomApiService

    /**
     * MineSiderService publiserer Kafka-meldingen i prod. Den mockes her (på samme måte som i
     * UngdomsytelsesøknadServiceTest) slik at scenariene kan styre suksess/feil ved "publisering"
     * uten å være avhengig av transaksjonell Kafka-producer i testkontekst.
     */
    @MockkBean
    lateinit var mineSiderService: MineSiderService

    @Autowired
    lateinit var deltakelseRepository: DeltakelseRepository

    @Autowired
    lateinit var registerService: UngdomsprogramregisterService

    @Autowired
    lateinit var ungdomsytelsesøknadService: UngdomsytelsesøknadService

    @Autowired
    lateinit var microfrontendRepository: MicrofrontendRepository

    @Autowired
    lateinit var søknadRepository: SøknadRepository

    @Autowired
    lateinit var taskService: TaskService

    @Autowired
    lateinit var taskWorker: TaskWorker

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var transactionManager: PlatformTransactionManager

    private val transactionTemplate by lazy { TransactionTemplate(transactionManager) }

    override val consumerGroupPrefix: String
        get() = "ungdomsytelsesoknad-transaksjon-test"

    override val consumerGroupTopics: List<String>
        get() = emptyList()

    @BeforeEach
    fun beforeEach() {
        every { ungBrukerdialogService.opprettSøkYtelseOppgave(any()) } returns true
        justRun { mineSiderService.aktiverMikrofrontend(any(), any(), any()) }
    }

    @Test
    fun `happy path - søknad, mikrofrontend og task committes atomisk i samme transaksjon`() {
        val søkerIdent = FødselsnummerGenerator.neste()
        mockPdlIdent(søkerIdent)

        val deltakelse = meldInnIProgrammet(søkerIdent)
        val antallTasksFør = antallAktiverMikrofrontendTasks(søkerIdent)

        transactionTemplate.executeWithoutResult {
            ungdomsytelsesøknadService.håndterMottattSøknad(
                lagUngdomsytelseSøknad(UUID.randomUUID().toString(), deltakelse.id!!, søkerIdent)
            )
        }

        assertThat(deltakelseRepository.findById(deltakelse.id!!).get().søktTidspunkt).isNotNull()
        val deltaker = deltakelseRepository.findById(deltakelse.id!!).get().deltaker
        assertThat(microfrontendRepository.findByDeltaker(deltaker)).isNotNull
        assertThat(søknadRepository.findAll().any { it.søkerIdent == søkerIdent }).isTrue()

        val task = finnSisteAktiverMikrofrontendTask(søkerIdent)
        assertThat(task).isNotNull
        assertThat(task!!.status).isEqualTo(Status.UBEHANDLET)
        assertThat(task.type).isEqualTo(AktiverMikrofrontendMinSideTask.TYPE)
        assertThat(antallAktiverMikrofrontendTasks(søkerIdent)).isEqualTo(antallTasksFør + 1)
    }

    @Test
    fun `rollback - hvis den omsluttende transaksjonen feiler, persisteres verken task eller mikrofrontend`() {
        val søkerIdent = FødselsnummerGenerator.neste()
        mockPdlIdent(søkerIdent)

        val deltakelse = meldInnIProgrammet(søkerIdent)
        val antallTasksFør = antallAktiverMikrofrontendTasks(søkerIdent)

        assertThrows<IllegalStateException> {
            transactionTemplate.executeWithoutResult {
                ungdomsytelsesøknadService.håndterMottattSøknad(
                    lagUngdomsytelseSøknad(UUID.randomUUID().toString(), deltakelse.id!!, søkerIdent)
                )
                // Simulerer feil i samme transaksjon etter at MicrofrontendService.sendOgLagre har lagret
                // task + mikrofrontend – hele transaksjonen skal rulles tilbake.
                error("Simulert feil i ytre transaksjon")
            }
        }

        // Alt innenfor den omsluttende transaksjonen skal være rullet tilbake:
        assertThat(deltakelseRepository.findById(deltakelse.id!!).get().søktTidspunkt).isNull()
        assertThat(søknadRepository.findAll().any { it.søkerIdent == søkerIdent }).isFalse()
        val deltaker = deltakelseRepository.findById(deltakelse.id!!).get().deltaker
        assertThat(microfrontendRepository.findByDeltaker(deltaker)).isNull()
        // Og aller viktigst: task'en (som ble skrevet via TaskService midt i transaksjonen) er rullet tilbake
        assertThat(antallAktiverMikrofrontendTasks(søkerIdent)).isEqualTo(antallTasksFør)
    }

    @Test
    fun `task plukkes opp i egen transaksjon og publiserer mikrofrontend-melding`() {
        val søkerIdent = FødselsnummerGenerator.neste()
        mockPdlIdent(søkerIdent)

        val deltakelse = meldInnIProgrammet(søkerIdent)

        transactionTemplate.executeWithoutResult {
            ungdomsytelsesøknadService.håndterMottattSøknad(
                lagUngdomsytelseSøknad(UUID.randomUUID().toString(), deltakelse.id!!, søkerIdent)
            )
        }

        val task = finnSisteAktiverMikrofrontendTask(søkerIdent)!!
        assertThat(task.status).isEqualTo(Status.UBEHANDLET)

        // Kjører task'en slik TaskStepExecutorService gjør i prod. doActualWork går i en egen
        // @Transactional(REQUIRES_NEW) – isolert fra den ytre transaksjonen som allerede er commited.
        taskWorker.markerPlukket(task.id)
        taskWorker.doActualWork(task.id)

        assertThat(taskService.findById(task.id).status).isEqualTo(Status.FERDIG)
        verify(exactly = 1) {
            mineSiderService.aktiverMikrofrontend(
                deltakerIdent = søkerIdent,
                microfrontendId = MicrofrontendId.UNGDOMSPROGRAMYTELSE_INNSYN,
                sensitivitet = Sensitivitet.HIGH,
            )
        }
    }

    @Test
    fun `task-feil ruller ikke tilbake allerede committede søknadsdata`() {
        val søkerIdent = FødselsnummerGenerator.neste()
        mockPdlIdent(søkerIdent)

        val deltakelse = meldInnIProgrammet(søkerIdent)

        transactionTemplate.executeWithoutResult {
            ungdomsytelsesøknadService.håndterMottattSøknad(
                lagUngdomsytelseSøknad(UUID.randomUUID().toString(), deltakelse.id!!, søkerIdent)
            )
        }

        val task = finnSisteAktiverMikrofrontendTask(søkerIdent)!!
        val deltakerEtterHåndtering = deltakelseRepository.findById(deltakelse.id!!).get().deltaker

        // Simulerer at Kafka-publiseringen feiler når task'en kjøres.
        every {
            mineSiderService.aktiverMikrofrontend(any(), any(), any())
        } throws RuntimeException("Simulert feil under publisering av mikrofrontend-melding")

        taskWorker.markerPlukket(task.id)
        val exception = assertThrows<Exception> { taskWorker.doActualWork(task.id) }
        taskWorker.doFeilhåndtering(task.id, exception)

        // Task-kjøringen feilet og skal planlegges på nytt (REQUIRES_NEW → isolert fra den ytre,
        // allerede commitede søknadstransaksjonen).
        val oppdatertTask = taskService.findById(task.id)
        assertThat(oppdatertTask.status).isEqualTo(Status.KLAR_TIL_PLUKK)
        assertThat(taskService.antallFeil(task.id)).isEqualTo(1)

        // Søknadsdataene som ble commited av den ytre transaksjonen skal være uendret:
        assertThat(deltakelseRepository.findById(deltakelse.id!!).get().søktTidspunkt).isNotNull()
        assertThat(søknadRepository.findAll().any { it.søkerIdent == søkerIdent }).isTrue()
        assertThat(microfrontendRepository.findByDeltaker(deltakerEtterHåndtering)).isNotNull
    }

    private fun antallAktiverMikrofrontendTasks(søkerIdent: String): Int =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM task WHERE type = ? AND payload LIKE ?",
            Int::class.java,
            AktiverMikrofrontendMinSideTask.TYPE,
            "%$søkerIdent%",
        ) ?: 0

    private fun finnSisteAktiverMikrofrontendTask(søkerIdent: String) =
        jdbcTemplate.query(
            "SELECT id FROM task WHERE type = ? AND payload LIKE ? ORDER BY id DESC LIMIT 1",
            { rs, _ -> rs.getLong("id") },
            AktiverMikrofrontendMinSideTask.TYPE,
            "%$søkerIdent%",
        ).firstOrNull()?.let { taskService.findById(it) }

    private fun lagUngdomsytelseSøknad(
        søknadId: String,
        deltakelseId: UUID,
        søkerIdent: String,
    ) = Ungdomsytelsesøknad(
        journalpostId = UUID.randomUUID().toString(),
        søknad = Søknad()
            .medSøknadId(søknadId)
            .medMottattDato(ZonedDateTime.now())
            .medSpråk(Språk.NORSK_BOKMÅL)
            .medKildesystem(Kildesystem.SØKNADSDIALOG)
            .medSøker(Søker(NorskIdentitetsnummer.of(søkerIdent)))
            .medYtelse(
                Ungdomsytelse()
                    .medSøknadType(UngSøknadstype.DELTAKELSE_SØKNAD)
                    .medStartdato(LocalDate.parse(DELTAKELSE_START))
                    .medDeltakelseId(deltakelseId)
            )
    )

    private fun meldInnIProgrammet(søkerIdent: String): DeltakelseDTO {
        val startdato = LocalDate.parse(DELTAKELSE_START)
        return registerService.leggTilIProgram(
            DeltakelseDTO(
                deltaker = DeltakerDTO(deltakerIdent = søkerIdent),
                fraOgMed = startdato,
                kvoteMaksDato = KvotePeriodeBeregner.beregn(startdato).tilOgMed
            )
        )
    }

    private fun mockPdlIdent(søkerIdent: String) {
        val pdlPerson = IdentInformasjon(
            ident = søkerIdent,
            historisk = false,
            gruppe = IdentGruppe.FOLKEREGISTERIDENT,
        )
        every { pdlService.hentFolkeregisteridenter(any()) } returns listOf(pdlPerson)
        every { pdlService.hentPerson(any()) } returns Scenarioer.lagPerson(LocalDate.of(2000, 1, 1))
        every { pdlService.hentAktørIder(any()) } returns listOf(
            IdentInformasjon("123456789", false, IdentGruppe.AKTORID)
        )
    }

    private companion object {
        const val DELTAKELSE_START = "2024-11-04"
    }
}
