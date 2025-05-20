package no.nav.ung.deltakelseopplyser.domene.inntekt.repository

import jakarta.persistence.EntityManager
import no.nav.k9.søknad.JsonUtils
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Kildesystem
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Språk
import no.nav.k9.søknad.ytelse.ung.v1.UngSøknadstype
import no.nav.k9.søknad.ytelse.ung.v1.Ungdomsytelse
import no.nav.k9.søknad.ytelse.ung.v1.inntekt.OppgittInntekt
import no.nav.ung.deltakelseopplyser.domene.soknad.kafka.Ungdomsytelsesøknad
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

        val rapportertInntektDAO = UngRapportertInntektDAO(
            journalpostId = "123",
            søkerIdent = "12345678901",
            opprettetDato = ZonedDateTime.now(),
            oppdatertDato = ZonedDateTime.now(),
            inntekt = JsonUtils.toString(
                lagInntektsrapporteringsSøknad(
                    søknadId = oppgaveReferanse,
                    søkerIdent = "12345678901"
                )
            ).also { println(it) },
        )

        rapportertInntektRepository.save(rapportertInntektDAO)
        entityManager.flush()

        val rapportertInntekt =
            rapportertInntektRepository.finnRapportertInntektGittOppgaveReferanse(oppgaveReferanse = oppgaveReferanse.toString())

        assertThat(rapportertInntekt).isNotNull
    }

    private fun lagInntektsrapporteringsSøknad(
        søknadId: UUID,
        søkerIdent: String,
    ) = Ungdomsytelsesøknad(
        journalpostId = "671161658",
        søknad = Søknad()
            .medSøknadId(søknadId.toString())
            .medMottattDato(ZonedDateTime.now())
            .medSpråk(Språk.NORSK_BOKMÅL)
            .medKildesystem(Kildesystem.SØKNADSDIALOG)
            .medSøker(Søker(NorskIdentitetsnummer.of(søkerIdent)))
            .medYtelse(
                Ungdomsytelse()
                    .medSøknadType(UngSøknadstype.RAPPORTERING_SØKNAD)
                    .medInntekter(OppgittInntekt(setOf()))
            )
    )
}
