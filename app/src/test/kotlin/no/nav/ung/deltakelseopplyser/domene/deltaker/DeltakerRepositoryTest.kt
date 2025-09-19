package no.nav.ung.deltakelseopplyser.domene.deltaker

import io.hypersistence.utils.hibernate.type.range.Range
import jakarta.persistence.EntityManager
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.EndretStartdatoOppgaveDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveDAO
import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseDAO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveStatus
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
import no.nav.ung.deltakelseopplyser.utils.FødselsnummerGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.util.*


@DataJpaTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension::class)
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE
)
class DeltakerRepositoryTest {

    @Autowired
    lateinit var deltakerRepository: DeltakerRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @Test
    fun `Forventer å finne deltakelse gitt oppgaveReferanse`() {
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

        val oppgaveReferanse = UUID.randomUUID()
        deltaker.leggTilOppgave(
            OppgaveDAO(
                id = UUID.randomUUID(),
                oppgaveReferanse = oppgaveReferanse,
                deltaker = deltaker,
                oppgavetype = Oppgavetype.BEKREFT_ENDRET_STARTDATO,
                oppgavetypeDataDAO = EndretStartdatoOppgaveDataDAO(
                    nyStartdato = LocalDate.now(),
                    forrigeStartdato = LocalDate.now().plusWeeks(1),
                ),
                status = OppgaveStatus.ULØST,
            )
        )

        entityManager.persist(deltaker)
        entityManager.flush()

        val resultat = deltakerRepository.finnDeltakerGittOppgaveReferanse(oppgaveReferanse)
        assertThat(resultat)
            .withFailMessage("Forventet å finne deltaker for oppgaveReferanse %s, men fikk null", oppgaveReferanse)
            .isNotNull
        assertThat(resultat!!.id)
            .withFailMessage("Forventet at deltaker id skulle være %s", deltaker.id)
            .isEqualTo(deltaker.id)
    }

    @Test
    fun `Tester at oppgave blir oppdatert`() {
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

        val oppgaveReferanse = UUID.randomUUID()
        deltaker.leggTilOppgave(
            OppgaveDAO(
                id = UUID.randomUUID(),
                oppgaveReferanse = oppgaveReferanse,
                deltaker = deltaker,
                oppgavetype = Oppgavetype.BEKREFT_ENDRET_STARTDATO,
                oppgavetypeDataDAO = EndretStartdatoOppgaveDataDAO(
                    nyStartdato = LocalDate.now(),
                    forrigeStartdato = LocalDate.now().plusWeeks(1),
                ),
                status = OppgaveStatus.ULØST,
            )
        )

        entityManager.persist(deltaker)
        entityManager.flush()

        val oppdatertDeltaker = deltakerRepository.findByDeltakerIdentIn(listOf(deltaker.deltakerIdent)).firstOrNull()?.let {
            assertThat(it.oppgaver).isNotEmpty
            val oppgaveDAO = it.oppgaver.first()
            oppgaveDAO.markerSomLøst()
            deltakerRepository.save(it)
        }

        assertThat(oppdatertDeltaker!!.oppgaver.first().status)
            .withFailMessage("Forventet at oppgave skulle være løst")
            .isEqualTo(OppgaveStatus.LØST)
    }
}
