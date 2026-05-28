package no.nav.ung.deltakelseopplyser.domene.register

import io.hypersistence.utils.hibernate.type.range.Range
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDAO
import no.nav.ung.deltakelseopplyser.integration.leader.LeaderElectorService
import no.nav.ung.deltakelseopplyser.utils.FødselsnummerGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AvsluttDeltakelseVedMaksdatoJobbTest {

    private val deltakelseRepository = mockk<DeltakelseRepository>()
    private val leaderElectorService = mockk<LeaderElectorService>()

    private val jobb = AvsluttDeltakelseVedMaksdatoJobb(
        deltakelseRepository = deltakelseRepository,
        leaderElectorService = leaderElectorService,
    )

    @Test
    fun `hopper over jobb naar vi ikke er leader`() {
        every { leaderElectorService.erLeader() } returns false

        jobb.avsluttDeltakelserVedMaksdato()

        verify(exactly = 0) { deltakelseRepository.findAktiveDeltakelserUtenSluttdato() }
        verify(exactly = 0) { deltakelseRepository.save(any()) }
    }

    @Test
    fun `avslutter kun deltakelser der beregnet maksdato er passert`() {
        every { leaderElectorService.erLeader() } returns true

        val gammelDeltakelse = aktivDeltakelseUtenSluttdato(LocalDate.now().minusYears(3))
        val fremtidigDeltakelse = aktivDeltakelseUtenSluttdato(LocalDate.now().plusYears(2))

        every { deltakelseRepository.findAktiveDeltakelserUtenSluttdato() } returns listOf(gammelDeltakelse, fremtidigDeltakelse)
        every { deltakelseRepository.save(any()) } answers { firstArg() }

        jobb.avsluttDeltakelserVedMaksdato()

        val forventetSluttdato = ForlengetPeriodeBeregner.beregn(
            gammelDeltakelse.getFom(),
            gammelDeltakelse.harForlengetPeriode
        ).tilOgMed

        verify(exactly = 1) { deltakelseRepository.save(gammelDeltakelse) }
        verify(exactly = 0) { deltakelseRepository.save(fremtidigDeltakelse) }
        assertThat(gammelDeltakelse.getTom()).isEqualTo(forventetSluttdato)
        assertThat(fremtidigDeltakelse.getTom()).isNull()
    }

    @Test
    fun `setter sluttdato lik beregnet maksdato for forlenget periode`() {
        val deltakelse = aktivDeltakelseUtenSluttdato(
            fom = LocalDate.of(2025, 1, 6),
            harForlengetPeriode = true,
        )
        every { deltakelseRepository.save(any()) } answers { firstArg() }

        jobb.avsluttEnkeltDeltakelse(deltakelse)

        val forventetSluttdato = ForlengetPeriodeBeregner.beregn(
            deltakelse.getFom(),
            harForlengetPeriode = true
        ).tilOgMed

        verify(exactly = 1) { deltakelseRepository.save(deltakelse) }
        assertThat(deltakelse.getTom()).isEqualTo(forventetSluttdato)
    }

    private fun aktivDeltakelseUtenSluttdato(
        fom: LocalDate,
        harForlengetPeriode: Boolean = false,
    ): DeltakelseDAO {
        val deltaker = DeltakerDAO(deltakerIdent = FødselsnummerGenerator.neste())
        return DeltakelseDAO(
            deltaker = deltaker,
            periode = Range.closedInfinite(fom),
            harForlengetPeriode = harForlengetPeriode,
        )
    }
}

