package no.nav.ung.deltakelseopplyser.statistikk.deltaker

import io.hypersistence.utils.hibernate.type.range.Range
import jakarta.persistence.EntityManager
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.EndretSluttdatoOppgaveDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.EndretStartdatoOppgaveDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.KontrollerRegisterInntektOppgaveTypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgavetypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.RegisterinntektDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.SøkYtelseOppgavetypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseDAO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveStatus
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
import no.nav.ung.deltakelseopplyser.utils.FødselsnummerGenerator
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

@DataJpaTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension::class)
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE
)
@Import(DeltakerStatistikkService::class)
class DeltakerStatistikkServiceTest {

    @Autowired
    lateinit var deltakerStatistikkService: DeltakerStatistikkService

    @Autowired
    lateinit var entityManager: EntityManager

    @BeforeEach
    fun setUp() {
        // Tømmer databasen før hver test
        entityManager.createNativeQuery("DELETE FROM oppgave").executeUpdate()
        entityManager.createNativeQuery("DELETE FROM ungdomsprogram_deltakelse").executeUpdate()
        entityManager.createNativeQuery("DELETE FROM min_side_microfrontend_status").executeUpdate()
        entityManager.createNativeQuery("DELETE FROM deltaker").executeUpdate()
    }

    @Test
    fun `Forventer korrekt antall deltakere i programmet`() {
        // Oppretter 5 deltakere i programmet
        repeat(5) {
            lagDeltakerIProgrammet()
        }

        // Oppretter 3 deltakere ferdig i programmet
        repeat(3) {
            lagDeltakerFerdigIProgrammet()
        }

        // Henter antall deltakere
        deltakerStatistikkService.antallDeltakereIUngdomsprogrammet().also { antallDeltakereRecord ->
            // Verifiserer at antall deltakere er 5
            assertThat(antallDeltakereRecord.antallDeltakere).isEqualTo(5L)
        }
    }


    @Test
    fun `antallDeltakereEtterAntallOppgaverFordeling skal returnere korrekt fordeling av deltakere per oppgaveantall`() {
        // Oppretter 10 deltakere med forskjellig antall oppgaver

        // 2 deltakere med ingen oppgaver
        repeat(2) {
            lagDeltakerIProgrammet()
        }

        // 4 deltakere med 1 oppgave
        repeat(4) {
            lagDeltakerMedOppgaver(
                Oppgavetype.SØK_YTELSE to SøkYtelseOppgavetypeDataDAO(
                    fomDato = LocalDate.now().plusDays(it.toLong())
                )
            )
        }

        // 3 deltakere med 2 oppgaver
        repeat(3) {
            lagDeltakerMedOppgaver(
                Oppgavetype.SØK_YTELSE to SøkYtelseOppgavetypeDataDAO(
                    fomDato = LocalDate.now().plusDays(it.toLong())
                ),
                Oppgavetype.BEKREFT_ENDRET_STARTDATO to EndretStartdatoOppgaveDataDAO(
                    nyStartdato = LocalDate.now().plusDays(it.toLong()),
                    forrigeStartdato = LocalDate.now().minusWeeks(1)
                )
            )
        }

        // 1 deltaker med 3 oppgaver
        lagDeltakerMedOppgaver(
            Oppgavetype.SØK_YTELSE to SøkYtelseOppgavetypeDataDAO(
                fomDato = LocalDate.now()
            ),
            Oppgavetype.BEKREFT_ENDRET_STARTDATO to EndretStartdatoOppgaveDataDAO(
                nyStartdato = LocalDate.now().plusDays(1),
                forrigeStartdato = LocalDate.now().minusWeeks(1)
            ),
            Oppgavetype.BEKREFT_AVVIK_REGISTERINNTEKT to KontrollerRegisterInntektOppgaveTypeDataDAO(
                registerinntekt = RegisterinntektDAO(
                    arbeidOgFrilansInntekter = listOf(),
                    ytelseInntekter = listOf()
                ),
                fomDato = LocalDate.now().minusWeeks(1),
                tomDato = LocalDate.now().minusWeeks(2)
            )
        )


        val fordeling = deltakerStatistikkService.antallDeltakereEtterAntallOppgaverFordeling()
        assertThat(fordeling).hasSize(4)

        // Sjekker at resultatet inneholder forventet fordeling
        assertThat(fordeling)
            .extracting(
                AntallDeltakereAntallOppgaverFordelingRecord::antallDeltakere,
                AntallDeltakereAntallOppgaverFordelingRecord::antallOppgaver,
            )
            .containsExactlyInAnyOrder(
                tuple(2L, 0L), // 2 Deltakere har 0 oppgaver
                tuple(4L, 1L), // 4 Deltakere har 1 oppgave
                tuple(3L, 2L), // 3 Deltakere har 2 oppgaver
                tuple(1L, 3L)  // 1 Deltaker har 3 oppgaver
            )
    }

    @Test
    fun `antallDeltakereEtterAntallOppgaverFordeling skal returnere tom liste når ingen deltakere finnes`() {
        // Ingen deltakere er opprettet, så resultatet bør være tomt
        val fordeling = deltakerStatistikkService.antallDeltakereEtterAntallOppgaverFordeling()

        assertThat(fordeling).isEmpty()
    }

    @Test
    fun `antallDeltakerePerOppgavetype skal returnere tom liste når ingen oppgaver finnes`() {
        // Oppretter deltakere uten oppgaver
        lagDeltakerIProgrammet()
        lagDeltakerIProgrammet()

        // Utfører søket
        val fordeling = deltakerStatistikkService.antallDeltakerePerOppgavetype()

        // Verifiserer resultatet
        assertThat(fordeling).isEmpty()
    }

    @Test
    fun `antallDeltakerePerOppgavetype skal returnere korrekt antall for flere deltakere med samme oppgavetype`() {
        // Oppretter deltakere med spesifikke oppgavetyper

        // 10 deltakere med SØK_YTELSE
        repeat(10) { index ->
            lagDeltakerMedOppgaver(
                Oppgavetype.SØK_YTELSE to SøkYtelseOppgavetypeDataDAO(
                    fomDato = LocalDate.now().plusDays(index.toLong())
                )
            )
        }

        // 7 deltakere med BEKREFT_ENDRET_STARTDATO
        repeat(7) { index ->
            lagDeltakerMedOppgaver(
                Oppgavetype.BEKREFT_ENDRET_STARTDATO to EndretStartdatoOppgaveDataDAO(
                    nyStartdato = LocalDate.now().plusDays(index.toLong()),
                    forrigeStartdato = LocalDate.now().minusWeeks(1)
                )
            )
        }

        // 5 deltakere med BEKREFT_AVVIK_REGISTERINNTEKT
        repeat(5) { index ->
            lagDeltakerMedOppgaver(
                Oppgavetype.BEKREFT_AVVIK_REGISTERINNTEKT to KontrollerRegisterInntektOppgaveTypeDataDAO(
                    registerinntekt = RegisterinntektDAO(
                        arbeidOgFrilansInntekter = listOf(),
                        ytelseInntekter = listOf()
                    ),
                    fomDato = LocalDate.now().minusWeeks(1),
                    tomDato = LocalDate.now().minusWeeks(2).plusDays(index.toLong())
                )
            )
        }

        // 3 deltakere med BEKREFT_ENDRET_SLUTTDATO
        repeat(3) { index ->
            lagDeltakerMedOppgaver(
                Oppgavetype.BEKREFT_ENDRET_SLUTTDATO to EndretSluttdatoOppgaveDataDAO(
                    nySluttdato = LocalDate.now().plusDays(index.toLong()),
                    forrigeSluttdato = LocalDate.now().minusWeeks(1)
                )
            )
        }

        val fordeling = deltakerStatistikkService.antallDeltakerePerOppgavetype()

        assertThat(fordeling).hasSize(4)
            .extracting(
                AntallDeltakerePerOppgavetypeRecord::oppgavetype,
                AntallDeltakerePerOppgavetypeRecord::antallDeltakere
            )
            .containsExactlyInAnyOrder(
                tuple("SØK_YTELSE", 10L), // 10 deltakere med SØK_YTELSE
                tuple("BEKREFT_ENDRET_STARTDATO", 7L), // 7 deltakere med BEKREFT_ENDRET_STARTDATO
                tuple("BEKREFT_AVVIK_REGISTERINNTEKT", 5L), // 5 deltakere med BEKREFT_AVVIK_REGISTERINNTEKT
                tuple("BEKREFT_ENDRET_SLUTTDATO", 3L) // 3 deltakere med BEKREFT_ENDRET_SLUTTDATO
            )
    }

    /**
     * Hjelpemetode for å opprette en deltaker uten oppgaver
     */
    private fun lagDeltakerIProgrammet(fom: LocalDate? = null, tom: LocalDate? = null): DeltakerDAO {
        val deltaker = DeltakerDAO(
            id = UUID.randomUUID(),
            deltakerIdent = FødselsnummerGenerator.neste(),
            deltakelseList = mutableListOf()
        )
        entityManager.persist(deltaker)
        entityManager.flush()

        lagDeltakelse(
            deltaker,
            fom ?: LocalDate.now().minusMonths(1),
            tom ?: LocalDate.now().plusMonths(1)
        )

        return deltaker
    }

    /**
     * Hjelpemetode for å opprette en deltaker som er ferdig i programmet
     * (dvs. har en deltakelse som er avsluttet)
     */
    private fun lagDeltakerFerdigIProgrammet(): DeltakerDAO {
        return lagDeltakerIProgrammet(LocalDate.now().minusMonths(2), LocalDate.now().minusDays(1))
    }

    private fun lagDeltakelse(
        deltaker: DeltakerDAO,
        fom: LocalDate,
        tom: LocalDate,
    ): DeltakelseDAO {
        val deltakelseDAO = DeltakelseDAO(
            id = UUID.randomUUID(),
            deltaker = deltaker,
            periode = Range.closed(fom, tom)
        )
        entityManager.persist(deltakelseDAO)
        entityManager.flush()
        return deltakelseDAO
    }


    /**
     * Hjelpemetode for å opprette en deltaker med oppgaver
     * @param oppgavePair Varargs-parameter med par av oppgavetype og oppgavetypedata
     */
    private fun lagDeltakerMedOppgaver(vararg oppgavePair: Pair<Oppgavetype, OppgavetypeDataDAO>): DeltakerDAO {
        val deltaker = lagDeltakerIProgrammet()

        oppgavePair.forEach { (oppgavetype, oppgavetypeData) ->
            deltaker.leggTilOppgave(opprettOppgave(deltaker, oppgavetype, oppgavetypeData))
        }

        entityManager.persist(deltaker)
        entityManager.flush()
        return deltaker
    }

    /**
     * Hjelpemetode for å opprette en oppgave
     */
    private fun opprettOppgave(
        deltaker: DeltakerDAO,
        oppgavetype: Oppgavetype,
        oppgavetypeDataDAO: OppgavetypeDataDAO,
    ): OppgaveDAO {
        return OppgaveDAO(
            id = UUID.randomUUID(),
            oppgaveReferanse = UUID.randomUUID(),
            deltaker = deltaker,
            oppgavetype = oppgavetype,
            oppgavetypeDataDAO = oppgavetypeDataDAO,
            status = OppgaveStatus.ULØST,
            opprettetDato = ZonedDateTime.now()
        )
    }
}
