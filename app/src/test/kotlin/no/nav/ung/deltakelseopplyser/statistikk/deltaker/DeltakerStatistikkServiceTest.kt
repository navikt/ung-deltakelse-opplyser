package no.nav.ung.deltakelseopplyser.statistikk.deltaker

import io.hypersistence.utils.hibernate.type.range.Range
import jakarta.persistence.EntityManager
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDAO
import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseDAO
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
    fun `Forventer korrekt antall deltakere i programmet som starter i fremtiden med åpen periode`() {
        // Oppretter 5 deltakere i programmet med åpen periode
        repeat(5) {
            lagDeltakerIProgrammet(
                fom = LocalDate.now().plusWeeks(1),
                tom = null // Ingen sluttdato, dvs. åpen periode
            )
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
        tom: LocalDate? = null,
    ): DeltakelseDAO {
        val periode = if (tom == null) {
            Range.closedInfinite(fom)
        } else {
            Range.closed(fom, tom)
        }
        val deltakelseDAO = DeltakelseDAO(
            id = UUID.randomUUID(),
            deltaker = deltaker,
            periode = periode
        )
        entityManager.persist(deltakelseDAO)
        entityManager.flush()
        return deltakelseDAO
    }
}
