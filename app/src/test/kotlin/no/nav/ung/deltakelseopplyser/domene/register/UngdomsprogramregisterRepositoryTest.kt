package no.nav.ung.deltakelseopplyser.domene.register

import io.hypersistence.utils.hibernate.type.range.Range
import jakarta.persistence.EntityManager
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDAO
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerRepository
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.EndretSluttdatoOppgavetypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.EndretStartdatoOppgavetypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveDAO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveStatus
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.util.*
import java.util.stream.Stream


@DataJpaTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension::class)
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE
)
class UngdomsprogramregisterRepositoryTest {

    @Autowired
    lateinit var repository: UngdomsprogramDeltakelseRepository

    @Autowired
    lateinit var deltakerRepository: DeltakerRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @BeforeEach
    fun setUp() {
        repository.deleteAll()

        // Forsikre oss om at btree_gist er installert
        val resultList = entityManager.createNativeQuery(
            "SELECT * FROM pg_extension WHERE extname = 'btree_gist'"
        ).resultList
        assertTrue(resultList.isNotEmpty())
    }

    @AfterAll
    internal fun tearDown() {
        repository.deleteAll()
    }

    @ParameterizedTest(name = "{index} => periodeA={0}, periodeB={1}, overlapper={2}")
    @MethodSource("perioderTilTest")
    fun testPeriodOverlaps(periodeA: Range<LocalDate>, periodeB: Range<LocalDate>, overlapper: Boolean) {
        val deltakerDAO = DeltakerDAO(deltakerIdent = "123")
        deltakerRepository.saveAndFlush(deltakerDAO)
        repository.saveAndFlush(
            UngdomsprogramDeltakelseDAO(
                deltaker = deltakerDAO,
                harSøkt = false,
                periode = periodeA
            )
        )

        if (overlapper) {
            assertThrows<DataIntegrityViolationException> {
                repository.saveAndFlush(
                    UngdomsprogramDeltakelseDAO(
                        deltaker = deltakerDAO,
                        harSøkt = false,
                        periode = periodeB
                    )
                )
            }
        } else {
            assertDoesNotThrow {
                repository.saveAndFlush(
                    UngdomsprogramDeltakelseDAO(
                        deltaker = deltakerDAO,
                        harSøkt = false,
                        periode = periodeB
                    )
                )
                entityManager.flush()
            }
        }
    }

    @Test
    fun `Forventer å finne deltakelse som starter på eksakt dato`() {
        val deltaker = DeltakerDAO(
            id = UUID.randomUUID(),
            deltakerIdent = "123",
            deltakelseList = mutableListOf(),
        )
        entityManager.persist(deltaker)
        val deltakerIdenter = listOf(deltaker.id)

        val førsteDeltakelseStartdato = LocalDate.of(2025, 1, 1)
        val førsteDeltakelse = UngdomsprogramDeltakelseDAO(
            deltaker = deltaker,
            harSøkt = false,
            periode = Range.closed(førsteDeltakelseStartdato, førsteDeltakelseStartdato.plusWeeks(1))
        )
        entityManager.persist(førsteDeltakelse)

        val andreDeltakelseStartdato = førsteDeltakelseStartdato.plusWeeks(1).plusDays(1)
        val andreDeltakelse = UngdomsprogramDeltakelseDAO(
            deltaker = deltaker,
            harSøkt = false,
            periode = Range.closedInfinite(andreDeltakelseStartdato)
        )
        entityManager.persist(andreDeltakelse)

        entityManager.flush()

        val førsteDeltakelseResultat = repository.finnDeltakelseSomStarter(deltakerIdenter, førsteDeltakelseStartdato)
        assertThat(førsteDeltakelseResultat)
            .withFailMessage("Forventet å finne deltakelse som starter på %s, men fikk null", førsteDeltakelseStartdato)
            .isNotNull
        assertThat(førsteDeltakelseResultat!!.getFom())
            .withFailMessage("Forventet at deltakelse skulle starte %s", førsteDeltakelseStartdato)
            .isEqualTo(førsteDeltakelseStartdato)

        val andreDeltakelseResultat = repository.finnDeltakelseSomStarter(deltakerIdenter, andreDeltakelseStartdato)
        assertThat(andreDeltakelseResultat)
            .withFailMessage("Forventet å finne deltakelse som starter %s, men fikk null", andreDeltakelseStartdato)
            .isNotNull
        assertThat(andreDeltakelseResultat!!.getFom())
            .withFailMessage("Forventet at deltakelse skulle starte %s", andreDeltakelseStartdato)
            .isEqualTo(andreDeltakelseStartdato)

        val ikkeEksisterendeStartdato = LocalDate.of(2025, 1, 1).minusDays(1)
        val ikkeEksisterendeDeltakelseResultat = repository.finnDeltakelseSomStarter(
            deltakerIdenter,
            ikkeEksisterendeStartdato
        )
        assertThat(ikkeEksisterendeDeltakelseResultat)
            .withFailMessage("Forventet å ikke finne deltakelse som starter %s", ikkeEksisterendeStartdato)
            .isNull()
    }

    @Test
    fun `Forventer å finne deltakelse gitt oppgaveId`() {
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

        val oppgaveId = UUID.randomUUID()
        deltakelse.leggTilOppgave(OppgaveDAO(
            id = oppgaveId,
            oppgaveReferanse = UUID.randomUUID(),
            deltakelse = deltakelse,
            oppgavetype = Oppgavetype.BEKREFT_ENDRET_STARTDATO,
            oppgavetypeDataDAO = EndretStartdatoOppgavetypeDataDAO(
                nyStartdato = LocalDate.now(),
                veilederRef = "abc-123",
                meldingFraVeileder = null
            ),
            status = OppgaveStatus.ULØST,
        ))
        deltakelse.leggTilOppgave(OppgaveDAO(
            id = UUID.randomUUID(),
            oppgaveReferanse = UUID.randomUUID(),
            deltakelse = deltakelse,
            oppgavetype = Oppgavetype.BEKREFT_ENDRET_SLUTTDATO,
            oppgavetypeDataDAO = EndretSluttdatoOppgavetypeDataDAO(
                nySluttdato = LocalDate.now(),
                veilederRef = "abc-123",
                meldingFraVeileder = null
            ),
            status = OppgaveStatus.ULØST,
        ))
        entityManager.persist(deltakelse)
        entityManager.flush()

        val resultat = repository.finnDeltakelseGittOppgaveId(oppgaveId)
        assertThat(resultat)
            .withFailMessage("Forventet å finne deltakelse for oppgaveId %s, men fikk null", oppgaveId)
            .isNotNull
        assertThat(resultat!!.id)
            .withFailMessage("Forventet at deltakelse id skulle være %s", deltakelse.id)
            .isEqualTo(deltakelse.id)
    }

    companion object {
        @JvmStatic
        fun perioderTilTest(): Stream<Arguments> {
            val idag = LocalDate.now()

            return Stream.of(
                /*
                 Delvis overlappende perioder gir feil
                               A
                 |-----------------------------|
                                 |-----------------------------|
                                               B
                 */
                Arguments.of(
                    Range.closed(idag, idag.plusDays(10)),
                    Range.closed(idag.plusDays(5), idag.plusDays(15)),
                    true
                ),

                /*
                 Delvis overlappende periode uten ende gir feil
                               A
                 |-----------------------------------------------------∞
                                 |--------------------------------|
                                               B
                 */
                Arguments.of(
                    Range.closedInfinite(idag),
                    Range.closed(idag.plusDays(5), idag.plusDays(15)),
                    true
                ),

                /*
                 Delvis overlappende perioder uten ende gir feil
                               A
                 |-----------------------------------------------------∞
                                 |-------------------------------------∞
                                               B
                 */
                Arguments.of(
                    Range.closedInfinite(idag),
                    Range.closedInfinite(idag.plusDays(5)),
                    true
                ),

                /*
                 Fullt overlappende perioder gir feil
                               A
                 |------------------------------------------------|
                               |----------------------|
                                          B
                 */
                Arguments.of(
                    Range.closed(idag, idag.plusDays(10)),
                    Range.closed(idag.plusDays(2), idag.plusDays(7)),
                    true
                ),

                /*
                 Like perioder gir feil
                                          A
                 |------------------------------------------------|
                 |------------------------------------------------|
                                          B
                 */
                Arguments.of(
                    Range.closed(idag, idag.plusDays(10)),
                    Range.closed(idag, idag.plusDays(10)),
                    true
                ),

                /*
                Perioder med overlapp på grensen til ny periode gir feil
                        A
                |---------------|
                                |----------------------|
                                            B
                 */
                Arguments.of(
                    Range.closed(idag, idag.plusDays(5)),
                    Range.closed(idag.plusDays(5), idag.plusDays(10)),
                    true
                ),

                /*
                Tilstøtende perioder uten overlapp gir ikke feil
                        A
                |---------------|
                                  |----------------------|
                                            B
                 */
                Arguments.of(
                    Range.closed(idag, idag.plusDays(5)),
                    Range.closed(idag.plusDays(6), idag.plusDays(10)),
                    false
                )
            )
        }
    }
}
