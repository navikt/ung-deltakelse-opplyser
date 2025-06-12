package no.nav.ung.deltakelseopplyser.domene.inntekt

import no.nav.k9.søknad.JsonUtils
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.ytelse.ung.v1.inntekt.OppgittInntekt
import no.nav.k9.søknad.ytelse.ung.v1.inntekt.OppgittInntektForPeriode
import no.nav.ung.deltakelseopplyser.domene.inntekt.repository.RapportertInntektRepository
import no.nav.ung.deltakelseopplyser.domene.inntekt.repository.UngRapportertInntektDAO
import no.nav.ung.deltakelseopplyser.domene.inntekt.utils.RapportertInntektUtils
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.BekreftelseDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.InntektsrapporteringOppgavetypeDataDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveStatus
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
import no.nav.ung.deltakelseopplyser.utils.FødselsnummerGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.math.BigDecimal
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
@Import(RapportertInntektService::class)
class RapportertInntektServiceTest {

    @Autowired
    private lateinit var rapportertInntektRepository: RapportertInntektRepository

    @Autowired
    private lateinit var rapportertInntektService: RapportertInntektService

    @Test
    fun `Gitt det eksisterer rapportert inntekt, forvent at oppgaven populeres med det`() {
        val deltakerIdent = FødselsnummerGenerator.neste()
        val oppgaveReferanse = UUID.randomUUID()
        val fraOgMed = LocalDate.parse("2025-01-01")
        val tilOgMed = LocalDate.parse("2025-01-31")

        // Gitt at det eksisterer rapportert inntekt for deltaker
        rapportertInntektRepository.save(rapportertInntekt(
            deltakerIdent = deltakerIdent,
            oppgaveReferanse = oppgaveReferanse,
            oppgittInntekt = OppgittInntekt(
                setOf(
                    OppgittInntektForPeriode(
                        BigDecimal.valueOf(10_000),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        Periode(fraOgMed, tilOgMed)
                    )
                )
            )
        ))

        // Når vi legger på rapportert inntekt på en oppgave
        val oppdatertOppgave: OppgaveDTO = rapportertInntektService.leggPåRapportertInntekt(
            OppgaveDTO(
                oppgaveReferanse = oppgaveReferanse,
                oppgavetype = Oppgavetype.RAPPORTER_INNTEKT,
                oppgavetypeData = InntektsrapporteringOppgavetypeDataDTO(
                    fraOgMed = fraOgMed,
                    tilOgMed = tilOgMed,
                    rapportertInntekt = null // Forventes oppdatert av tjenesten
                ),
                bekreftelse = BekreftelseDTO(
                    harGodtattEndringen = true,
                    uttalelseFraBruker = null
                ),
                status = OppgaveStatus.LØST,
                opprettetDato = ZonedDateTime.now(),
                løstDato = ZonedDateTime.now(),
                åpnetDato = ZonedDateTime.now(),
                lukketDato = null,
                frist = ZonedDateTime.now().plusDays(14)
            )
        )
        // Forvent at oppgaven er oppdatert med rapportert inntekt
        assertThat(oppdatertOppgave.oppgavetypeData).isInstanceOf(InntektsrapporteringOppgavetypeDataDTO::class.java)
        val inntektsrapporteringData = oppdatertOppgave.oppgavetypeData as InntektsrapporteringOppgavetypeDataDTO
        assertThat(inntektsrapporteringData.rapportertInntekt).isNotNull

        val rapportertInntekt = inntektsrapporteringData.rapportertInntekt!!
        assertThat(rapportertInntekt.fraOgMed).isEqualTo(fraOgMed)
        assertThat(rapportertInntekt.tilOgMed).isEqualTo(tilOgMed)
        assertThat(rapportertInntekt.arbeidstakerOgFrilansInntekt).isEqualTo(BigDecimal.valueOf(10_000))
    }

    @Test
    fun `Gitt det ikke eksisterer rapportert inntekt, forvent at oppgaven returneres uten endringer`() {

        // Når vi legger på rapportert inntekt på en oppgave uten eksisterende rapportert inntekt
        val oppgaveDTO = OppgaveDTO(
            oppgaveReferanse = UUID.randomUUID(),
            oppgavetype = Oppgavetype.RAPPORTER_INNTEKT,
            oppgavetypeData = InntektsrapporteringOppgavetypeDataDTO(
                fraOgMed = LocalDate.now(),
                tilOgMed = LocalDate.now().plusDays(30),
                rapportertInntekt = null // Forventes oppdatert av tjenesten
            ),
            bekreftelse = BekreftelseDTO(
                harGodtattEndringen = true,
                uttalelseFraBruker = null
            ),
            status = OppgaveStatus.LØST,
            opprettetDato = ZonedDateTime.now(),
            løstDato = ZonedDateTime.now(),
            åpnetDato = ZonedDateTime.now(),
            lukketDato = null,
            frist = ZonedDateTime.now().plusDays(14)
        )

        val oppdatertOppgave: OppgaveDTO = rapportertInntektService.leggPåRapportertInntekt(oppgaveDTO)

        // Forvent at oppgaven er uendret
        assertThat(oppdatertOppgave).isEqualTo(oppgaveDTO)
    }


    private fun rapportertInntekt(
        deltakerIdent: String,
        oppgaveReferanse: UUID,
        oppgittInntekt: OppgittInntekt,
    ) = UngRapportertInntektDAO(
        journalpostId = "123456789",
        søkerIdent = deltakerIdent,
        opprettetDato = ZonedDateTime.now(),
        oppdatertDato = ZonedDateTime.now(),
        inntekt = JsonUtils.toString(
            RapportertInntektUtils.lagInntektsrapporteringsSøknad(
                oppgaveReferanse = oppgaveReferanse,
                deltakerIdent = deltakerIdent,
                oppgittInntekt = oppgittInntekt
            )
        )
    )
}
