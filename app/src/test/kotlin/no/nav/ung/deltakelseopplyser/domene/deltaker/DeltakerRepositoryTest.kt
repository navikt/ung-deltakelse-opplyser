package no.nav.ung.deltakelseopplyser.domene.deltaker

import io.hypersistence.utils.hibernate.type.range.Range
import jakarta.persistence.EntityManager
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.EndretSluttdatoOppgavetypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.EndretStartdatoOppgavetypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveDAO
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramDeltakelseDAO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveStatus
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
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

    @BeforeEach
    fun setUp() {
        deltakerRepository.deleteAll()
    }

    @AfterAll
    internal fun tearDown() {
        deltakerRepository.deleteAll()
    }

    @Test
    fun `Forventer å finne deltakelse gitt oppgaveReferanse`() {
        val deltaker = DeltakerDAO(
            id = UUID.randomUUID(),
            deltakerIdent = "123",
            deltakelseList = mutableListOf(),
        )
        entityManager.persist(deltaker)

        val deltakelse = UngdomsprogramDeltakelseDAO(
            deltaker = deltaker,
            harSøkt = false,
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
                oppgavetypeDataDAO = EndretStartdatoOppgavetypeDataDAO(
                    nyStartdato = LocalDate.now(),
                    veilederRef = "abc-123",
                    meldingFraVeileder = null
                ),
                status = OppgaveStatus.ULØST,
            )
        )

        deltaker.leggTilOppgave(
            OppgaveDAO(
                id = UUID.randomUUID(),
                oppgaveReferanse = UUID.randomUUID(),
                deltaker = deltaker,
                oppgavetype = Oppgavetype.BEKREFT_ENDRET_SLUTTDATO,
                oppgavetypeDataDAO = EndretSluttdatoOppgavetypeDataDAO(
                    nySluttdato = LocalDate.now(),
                    veilederRef = "abc-123",
                    meldingFraVeileder = null
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
}
