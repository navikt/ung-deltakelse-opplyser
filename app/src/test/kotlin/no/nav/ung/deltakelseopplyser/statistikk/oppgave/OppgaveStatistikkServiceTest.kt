package no.nav.ung.deltakelseopplyser.statistikk.oppgave

import io.hypersistence.utils.hibernate.type.range.Range
import jakarta.persistence.EntityManager
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.EndretStartdatoOppgaveDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.KontrollerRegisterInntektOppgaveTypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.RegisterinntektDAO
import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseDAO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.*
import no.nav.ung.deltakelseopplyser.utils.FødselsnummerGenerator
import org.assertj.core.api.Assertions.assertThat
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
@Import(OppgaveStatistikkService::class)
class OppgaveStatistikkServiceTest {


    @Autowired
    lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var oppgaveStatistikkService: OppgaveStatistikkService

    @BeforeEach
    fun setUp() {
        entityManager.createNativeQuery("DELETE FROM Oppgave").executeUpdate()
    }

    @Test
    fun `Skal ikke finne oppgave som er uløst og 13 dager gammel`() {
        val deltaker = lagDeltakelse()

        deltaker.leggTilOppgave(uløstOppgave(deltaker, 13))

        val oppgaverMedSvarEllerEldreEnn14Dager = oppgaveStatistikkService.oppgaverMedSvarEllerEldreEnn14Dager()

        assertThat(oppgaverMedSvarEllerEldreEnn14Dager.size).isEqualTo(0)

    }

    @Test
    fun `Skal finne oppgave som er uløst om mer en 14 dager gammel`() {
        val deltaker = lagDeltakelse()

        deltaker.leggTilOppgave(
            uløstOppgave(deltaker, 15)
        )

        val oppgaverMedSvarEllerEldreEnn14Dager = oppgaveStatistikkService.oppgaverMedSvarEllerEldreEnn14Dager()

        assertThat(oppgaverMedSvarEllerEldreEnn14Dager.size).isEqualTo(1)
        assertThat(oppgaverMedSvarEllerEldreEnn14Dager[0].oppgaveType).isEqualTo(Oppgavetype.BEKREFT_ENDRET_STARTDATO)
        assertThat(oppgaverMedSvarEllerEldreEnn14Dager[0].antall).isEqualTo(1)
        assertThat(oppgaverMedSvarEllerEldreEnn14Dager[0].svartidAntallDager).isNull()
        assertThat(oppgaverMedSvarEllerEldreEnn14Dager[0].erLøst).isFalse()
        assertThat(oppgaverMedSvarEllerEldreEnn14Dager[0].erLukket).isFalse()
        assertThat(oppgaverMedSvarEllerEldreEnn14Dager[0].ikkeMottattOgEldreEnn14Dager).isTrue()
        assertThat(oppgaverMedSvarEllerEldreEnn14Dager[0].opprettetTidspunkt).isNotNull

    }

    private fun uløstOppgave(deltaker: DeltakerDAO, alderAntallDager: Long) = OppgaveDAO(
        id = UUID.randomUUID(),
        oppgaveReferanse = UUID.randomUUID(),
        deltaker = deltaker,
        oppgavetype = Oppgavetype.BEKREFT_ENDRET_STARTDATO,
        oppgavetypeDataDAO = EndretStartdatoOppgaveDataDAO(
            nyStartdato = LocalDate.now(),
            forrigeStartdato = LocalDate.now().plusWeeks(1),
        ),
        status = OppgaveStatus.ULØST,
        opprettetDato = ZonedDateTime.now().minusDays(alderAntallDager)
    )

    @Test
    fun `Skal finne oppgave som er løst 2 dager etter opprettet tidspunkt`() {
        val deltaker = lagDeltakelse()

        deltaker.leggTilOppgave(endretStartdatoOppgaveMedSvar(deltaker, 2))

        val oppgaverMedSvarEllerEldreEnn14Dager = oppgaveStatistikkService.oppgaverMedSvarEllerEldreEnn14Dager()

        assertThat(oppgaverMedSvarEllerEldreEnn14Dager.size).isEqualTo(1)
        assertThat(oppgaverMedSvarEllerEldreEnn14Dager[0].oppgaveType).isEqualTo(Oppgavetype.BEKREFT_ENDRET_STARTDATO)
        assertThat(oppgaverMedSvarEllerEldreEnn14Dager[0].antall).isEqualTo(1)
        assertThat(oppgaverMedSvarEllerEldreEnn14Dager[0].svartidAntallDager).isEqualTo(2)
        assertThat(oppgaverMedSvarEllerEldreEnn14Dager[0].erLøst).isTrue()
        assertThat(oppgaverMedSvarEllerEldreEnn14Dager[0].erLukket).isFalse()
        assertThat(oppgaverMedSvarEllerEldreEnn14Dager[0].ikkeMottattOgEldreEnn14Dager).isFalse()
        assertThat(oppgaverMedSvarEllerEldreEnn14Dager[0].opprettetTidspunkt).isNotNull
    }

    @Test
    fun `Skal finne oppgave som er lukket 2 dager etter opprettet tidspunkt`() {
        val deltaker = lagDeltakelse()

        deltaker.leggTilOppgave(endretStartdatoOppgaveLukket(deltaker, 2))

        val oppgaverMedSvarEllerEldreEnn14Dager = oppgaveStatistikkService.oppgaverMedSvarEllerEldreEnn14Dager()

        assertThat(oppgaverMedSvarEllerEldreEnn14Dager.size).isEqualTo(1)
        assertThat(oppgaverMedSvarEllerEldreEnn14Dager[0].oppgaveType).isEqualTo(Oppgavetype.BEKREFT_ENDRET_STARTDATO)
        assertThat(oppgaverMedSvarEllerEldreEnn14Dager[0].antall).isEqualTo(1)
        assertThat(oppgaverMedSvarEllerEldreEnn14Dager[0].svartidAntallDager).isEqualTo(2)
        assertThat(oppgaverMedSvarEllerEldreEnn14Dager[0].erLøst).isFalse()
        assertThat(oppgaverMedSvarEllerEldreEnn14Dager[0].erLukket).isTrue()
        assertThat(oppgaverMedSvarEllerEldreEnn14Dager[0].ikkeMottattOgEldreEnn14Dager).isFalse()
        assertThat(oppgaverMedSvarEllerEldreEnn14Dager[0].opprettetTidspunkt).isNotNull
    }

    private fun endretStartdatoOppgaveLukket(deltaker: DeltakerDAO, antallDager: Long): OppgaveDAO {
        val opprettetDato = ZonedDateTime.now().minusDays(15)
        return OppgaveDAO(
            id = UUID.randomUUID(),
            oppgaveReferanse = UUID.randomUUID(),
            deltaker = deltaker,
            oppgavetype = Oppgavetype.BEKREFT_ENDRET_STARTDATO,
            oppgavetypeDataDAO = EndretStartdatoOppgaveDataDAO(
                nyStartdato = LocalDate.now(),
                forrigeStartdato = LocalDate.now().plusWeeks(1),
            ),
            status = OppgaveStatus.LUKKET,
            opprettetDato = opprettetDato,
            lukketDato = opprettetDato.plusDays(antallDager)
        )
    }

    @Test
    fun `Skal finne to oppgaver av ulik type som er løst 2 dager etter opprettet tidspunkt`() {
        val deltaker = lagDeltakelse()
        deltaker.leggTilOppgave(endretStartdatoOppgaveMedSvar(deltaker, 2))
        deltaker.leggTilOppgave(kontrollerRegisterinntektOppgaveMedSvar(deltaker, 2))

        val oppgaverMedSvarEllerEldreEnn14Dager = oppgaveStatistikkService.oppgaverMedSvarEllerEldreEnn14Dager()

        assertThat(oppgaverMedSvarEllerEldreEnn14Dager.size).isEqualTo(2)
        val bekreftetStartdatoOppgave = oppgaverMedSvarEllerEldreEnn14Dager.stream()
            .filter { it.oppgaveType == Oppgavetype.BEKREFT_ENDRET_STARTDATO }
            .findFirst()
            .orElseThrow { AssertionError("Forventet å finne oppgave av type BEKREFT_ENDRET_STARTDATO") }
        assertThat(bekreftetStartdatoOppgave.antall).isEqualTo(1)
        assertThat(bekreftetStartdatoOppgave.svartidAntallDager).isEqualTo(2)
        assertThat(bekreftetStartdatoOppgave.erLøst).isTrue()
        assertThat(bekreftetStartdatoOppgave.erLukket).isFalse()
        assertThat(bekreftetStartdatoOppgave.ikkeMottattOgEldreEnn14Dager).isFalse()
        assertThat(bekreftetStartdatoOppgave.opprettetTidspunkt).isNotNull

        val bekreftetRegisterinntektOppgave = oppgaverMedSvarEllerEldreEnn14Dager.stream()
            .filter { it.oppgaveType == Oppgavetype.BEKREFT_AVVIK_REGISTERINNTEKT }
            .findFirst()
            .orElseThrow { AssertionError("Forventet å finne oppgave av type BEKREFT_AVVIK_REGISTERINNTEKT") }
        assertThat(bekreftetRegisterinntektOppgave.antall).isEqualTo(1)
        assertThat(bekreftetRegisterinntektOppgave.svartidAntallDager).isEqualTo(2)
        assertThat(bekreftetRegisterinntektOppgave.erLøst).isTrue()
        assertThat(bekreftetRegisterinntektOppgave.erLukket).isFalse()
        assertThat(bekreftetRegisterinntektOppgave.ikkeMottattOgEldreEnn14Dager).isFalse()
        assertThat(bekreftetRegisterinntektOppgave.opprettetTidspunkt).isNotNull
    }

    private fun lagDeltakelse(): DeltakerDAO {
        val deltaker = DeltakerDAO(
            id = UUID.randomUUID(),
            deltakerIdent = FødselsnummerGenerator.neste(),
            deltakelseList = mutableListOf(),
        )
        entityManager.persist(deltaker)

        val deltakelse = DeltakelseDAO(
            deltaker = deltaker,
            periode = Range.closed(LocalDate.now(), LocalDate.now().plusWeeks(1))
        )
        entityManager.persist(deltakelse)
        return deltaker
    }

    private fun kontrollerRegisterinntektOppgaveMedSvar(deltaker: DeltakerDAO, antallDager: Long): OppgaveDAO {
        val opprettetDato = ZonedDateTime.now().minusDays(15)
        return OppgaveDAO(
            id = UUID.randomUUID(),
            oppgaveReferanse = UUID.randomUUID(),
            deltaker = deltaker,
            oppgavetype = Oppgavetype.BEKREFT_AVVIK_REGISTERINNTEKT,
            oppgavetypeDataDAO = KontrollerRegisterInntektOppgaveTypeDataDAO(
                registerinntekt = RegisterinntektDAO(
                    arbeidOgFrilansInntekter = listOf(),
                    ytelseInntekter = listOf()
                ),
                fomDato = LocalDate.now(),
                tomDato = LocalDate.now().plusWeeks(1),
                gjelderSisteMåned = false
            ),
            status = OppgaveStatus.LØST,
            opprettetDato = opprettetDato,
            løstDato = opprettetDato.plusDays(antallDager)
        )
    }

    private fun endretStartdatoOppgaveMedSvar(deltaker: DeltakerDAO, antallDager: Long): OppgaveDAO {
        val opprettetDato = ZonedDateTime.now().minusDays(15)
        return OppgaveDAO(
            id = UUID.randomUUID(),
            oppgaveReferanse = UUID.randomUUID(),
            deltaker = deltaker,
            oppgavetype = Oppgavetype.BEKREFT_ENDRET_STARTDATO,
            oppgavetypeDataDAO = EndretStartdatoOppgaveDataDAO(
                nyStartdato = LocalDate.now(),
                forrigeStartdato = LocalDate.now().plusWeeks(1),
            ),
            status = OppgaveStatus.LØST,
            opprettetDato = opprettetDato,
            løstDato = opprettetDato.plusDays(antallDager)
        )
    }


}
