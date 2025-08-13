package no.nav.ung.deltakelseopplyser.statistikk.bigquery

import com.ninjasquad.springmockk.MockkBean
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveStatus
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
import no.nav.ung.deltakelseopplyser.statistikk.deltaker.AntallDeltakerePerOppgavetypeRecord
import no.nav.ung.deltakelseopplyser.statistikk.deltaker.AntallDeltakerePerOppgavetypeTabell
import no.nav.ung.deltakelseopplyser.statistikk.deltaker.AntallDeltakereIUngdomsprogrammetRecord
import no.nav.ung.deltakelseopplyser.statistikk.deltaker.AntallDeltakereTabell
import no.nav.ung.deltakelseopplyser.statistikk.oppgave.OppgaveSvartidRecord
import no.nav.ung.deltakelseopplyser.statistikk.oppgave.OppgaveSvartidTabell
import no.nav.ung.deltakelseopplyser.utils.TokenTestUtils.mockContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.ZonedDateTime

@ActiveProfiles("test")
@EnableMockOAuth2Server
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ExtendWith(SpringExtension::class)
@Import(BigQueryTestConfiguration::class)
class BigQueryKlientTest {

    @Autowired
    lateinit var bigQueryKlient: BigQueryKlient

    @MockkBean
    private lateinit var springTokenValidationContextHolder: SpringTokenValidationContextHolder


    @BeforeEach
    fun setUp() {
        springTokenValidationContextHolder.mockContext()
    }


    @Test
    fun `Skal kunne publisere svartidstatistikk`() {
        val record = OppgaveSvartidRecord(1L, true, false, false, Oppgavetype.SØK_YTELSE, 100, ZonedDateTime.now())
        bigQueryKlient.publish(BigQueryTestConfiguration.BIG_QUERY_DATASET, OppgaveSvartidTabell, listOf(record))
    }

    @Test
    fun `Skal kunne publisere antall deltakere i programmet statistikk`() {
        val record = AntallDeltakereIUngdomsprogrammetRecord(10L, ZonedDateTime.now())
        bigQueryKlient.publish(BigQueryTestConfiguration.BIG_QUERY_DATASET, AntallDeltakereTabell, listOf(record))
    }

    @Test
    fun `Skal kunne publisere antall deltakere per oppgavetype fordeling statistikk`() {
        val records = listOf(
            AntallDeltakerePerOppgavetypeRecord(
                Oppgavetype.SØK_YTELSE.name,
                OppgaveStatus.ULØST.name,
                8,
                ZonedDateTime.now()
            ),
            AntallDeltakerePerOppgavetypeRecord(
                Oppgavetype.RAPPORTER_INNTEKT.name,
                OppgaveStatus.ULØST.name,
                3,
                ZonedDateTime.now(),
            ),
            AntallDeltakerePerOppgavetypeRecord(
                Oppgavetype.BEKREFT_AVVIK_REGISTERINNTEKT.name,
                OppgaveStatus.ULØST.name,
                1,
                ZonedDateTime.now(),
            ),
        )
        bigQueryKlient.publish(
            BigQueryTestConfiguration.BIG_QUERY_DATASET,
            AntallDeltakerePerOppgavetypeTabell, records
        )
    }

}
