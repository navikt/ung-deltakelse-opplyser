package no.nav.ung.deltakelseopplyser.domene.inntekt

import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.k9.søknad.JsonUtils
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.ytelse.ung.v1.Ungdomsytelse
import no.nav.k9.søknad.ytelse.ung.v1.inntekt.OppgittInntektForPeriode
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerService
import no.nav.ung.deltakelseopplyser.domene.inntekt.repository.RapportertInntektRepository
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramDeltakelseDAO
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.RapportPeriodeinfoDTO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.Period

@Service
class RapportertInntektService(
    private val rapportertInntektRepository: RapportertInntektRepository,
    private val deltakerService: DeltakerService,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RapportertInntektService::class.java)

        fun UngdomsprogramDeltakelseDAO.rapporteringsPerioder(): List<RapportPeriodeinfoDTO> {
            val startDato = getFom().withDayOfMonth(1)
            val sluttDato = getTom() ?: LocalDate.now()
            val tidslinje = LocalDateTimeline(getFom(), sluttDato, this)
            val månedTidslinje = tidslinje.splitAtRegular(startDato, tidslinje.maxLocalDate, Period.ofMonths(1))
            return månedTidslinje.toSegments().map { segment ->
                RapportPeriodeinfoDTO(
                    fraOgMed = segment.fom,
                    tilOgMed = segment.tom,
                    harRapportert = false
                )
            }
        }
    }

    fun hentRapporteringsperioder(deltakelse: UngdomsprogramDeltakelseDAO): List<RapportPeriodeinfoDTO> {
        val deltakerIdenter = deltakerService.hentDeltakterIdenter(deltakelse.deltaker.deltakerIdent)
        val rapporterteInntekter = rapportertInntektRepository
            .findBySøkerIdentIn(deltakerIdenter)
            .flatMap { oppgittInntekt ->
                JsonUtils.fromString(oppgittInntekt.inntekt, Søknad::class.java)
                    .getYtelse<Ungdomsytelse>()
                    .inntekter.oppgittePeriodeinntekter
            }

        logger.info(
            "Fant ${rapporterteInntekter.size} rapporterte inntekter for deltakelse ${deltakelse.id}. Perioder: {}",
            rapporterteInntekter.map { it.periode })

        return deltakelse.rapporteringsPerioder()
            .map { rapporteringsPeriode ->
                rapporterteInntekter
                    .find {
                        logger.info("Sjekker om inntekt ${it.periode} er innenfor periode $rapporteringsPeriode")
                        it.periode.fraOgMed >= rapporteringsPeriode.fraOgMed && it.periode.tilOgMed <= rapporteringsPeriode.tilOgMed
                    }
                    ?.let { inntekt: OppgittInntektForPeriode ->
                        RapportPeriodeinfoDTO(
                            fraOgMed = rapporteringsPeriode.fraOgMed,
                            tilOgMed = rapporteringsPeriode.tilOgMed,
                            harRapportert = true,
                            arbeidstakerOgFrilansInntekt = inntekt.arbeidstakerOgFrilansInntekt,
                            inntektFraYtelse = inntekt.ytelse
                        )
                    } ?: rapporteringsPeriode
            }
    }
}
