package no.nav.ung.deltakelseopplyser.domene.register

import io.hypersistence.utils.hibernate.type.range.Range
import jakarta.persistence.EntityManager
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDAO
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerRepository
import no.nav.ung.deltakelseopplyser.utils.FødselsnummerGenerator
import org.assertj.core.api.Assertions.assertThat
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
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
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
    lateinit var repository: DeltakelseRepository

    @Autowired
    lateinit var deltakerRepository: DeltakerRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @BeforeEach
    fun setUp() {
        // Forsikre oss om at btree_gist er installert
        val resultList = entityManager.createNativeQuery(
            "SELECT * FROM pg_extension WHERE extname = 'btree_gist'"
        ).resultList
        assertTrue(resultList.isNotEmpty())
    }

    @ParameterizedTest(name = "{index} => periodeA={0}, periodeB={1}, overlapper={2}")
    @MethodSource("perioderTilTest")
    fun testPeriodOverlaps(periodeA: Range<LocalDate>, periodeB: Range<LocalDate>, overlapper: Boolean) {
        val deltakerDAO = DeltakerDAO(deltakerIdent = FødselsnummerGenerator.neste())
        deltakerRepository.saveAndFlush(deltakerDAO)
        repository.saveAndFlush(
            DeltakelseDAO(
                deltaker = deltakerDAO,
                periode = periodeA
            )
        )

        if (overlapper) {
            assertThrows<DataIntegrityViolationException> {
                repository.saveAndFlush(
                    DeltakelseDAO(
                        deltaker = deltakerDAO,
                        periode = periodeB
                    )
                )
            }
        } else {
            assertDoesNotThrow {
                repository.saveAndFlush(
                    DeltakelseDAO(
                        deltaker = deltakerDAO,
                        periode = periodeB
                    )
                )
                entityManager.flush()
            }
        }
    }

    @Test
    fun `findAktiveDeltakelserUtenSluttdato returnerer kun ikke-slettede deltakelser uten sluttdato`() {
        val iDag = LocalDate.now()

        val aktivDeltakelse = opprettDeltakelse(
            periode = Range.closedInfinite(iDag.minusMonths(3)),
            erSlettet = false,
        )
        val avsluttetDeltakelse = opprettDeltakelse(
            periode = Range.closed(iDag.minusMonths(6), iDag.minusDays(1)),
            erSlettet = false,
        )
        val slettetAktivDeltakelse = opprettDeltakelse(
            periode = Range.closedInfinite(iDag.minusMonths(2)),
            erSlettet = true,
        )

        val resultat = repository.findAktiveDeltakelserUtenSluttdato()

        assertThat(resultat.map { it.id })
            .contains(aktivDeltakelse.id)
            .doesNotContain(avsluttetDeltakelse.id, slettetAktivDeltakelse.id)
    }

    private fun opprettDeltakelse(
        periode: Range<LocalDate>,
        erSlettet: Boolean,
    ): DeltakelseDAO {
        val deltakerDAO = DeltakerDAO(deltakerIdent = FødselsnummerGenerator.neste())
        deltakerRepository.saveAndFlush(deltakerDAO)

        return repository.saveAndFlush(
            DeltakelseDAO(
                deltaker = deltakerDAO,
                periode = periode,
                erSlettet = erSlettet,
            )
        )
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
