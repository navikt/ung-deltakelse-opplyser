package no.nav.ung.deltakelseopplyser.domene.inntekt.repository

import jakarta.persistence.EntityManager
import no.nav.k9.søknad.JsonUtils
import no.nav.k9.søknad.ytelse.ung.v1.inntekt.OppgittInntekt
import no.nav.ung.deltakelseopplyser.domene.inntekt.utils.RapportertInntektUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.ZonedDateTime
import java.util.*

@DataJpaTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension::class)
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE
)
class RapportertInntektRepositoryTest {

    @Autowired
    lateinit var rapportertInntektRepository: RapportertInntektRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @BeforeEach
    fun setUp() {
        rapportertInntektRepository.deleteAll()
    }

    @Test
    fun `Forventer å kunne hente opp rapportert inntekt gitt oppgavereferanse`() {
        val oppgaveReferanse = UUID.randomUUID()
        val deltakerIdent = "12345678901"

        val rapportertInntektDAO = UngRapportertInntektDAO(
            journalpostId = "123",
            søkerIdent = deltakerIdent,
            opprettetDato = ZonedDateTime.now(),
            oppdatertDato = ZonedDateTime.now(),
            inntekt = JsonUtils.toString(
                RapportertInntektUtils.lagInntektsrapporteringsSøknad(
                    oppgaveReferanse = oppgaveReferanse,
                    deltakerIdent = deltakerIdent,
                    oppgittInntekt = OppgittInntekt(setOf())
                )
            ).also { println(it) },
        )

        rapportertInntektRepository.save(rapportertInntektDAO)
        entityManager.flush()

        val rapportertInntekt =
            rapportertInntektRepository.finnRapportertInntektGittOppgaveReferanse(oppgaveReferanse = oppgaveReferanse.toString())

        assertThat(rapportertInntekt).isNotNull
    }
}
