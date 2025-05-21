package no.nav.ung.deltakelseopplyser.domene.inntekt

import no.nav.k9.søknad.JsonUtils
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.ytelse.ung.v1.Ungdomsytelse
import no.nav.ung.deltakelseopplyser.domene.inntekt.repository.RapportertInntektRepository
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.RapportertInntektPeriodeinfoDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.InntektsrapporteringOppgavetypeDataDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveDTO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RapportertInntektService(
    private val rapportertInntektRepository: RapportertInntektRepository,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RapportertInntektService::class.java)
    }

    fun leggPåRapportertInntekt(oppgaveDTO: OppgaveDTO): OppgaveDTO {
        return rapportertInntektRepository
            .finnRapportertInntektGittOppgaveReferanse(oppgaveDTO.oppgaveReferanse.toString())
            ?.let { ungRapportertInntektDAO ->
                JsonUtils.fromString(ungRapportertInntektDAO.inntekt, Søknad::class.java)
            }?.rapportertInntektPeriodeInfo()
            ?.also { logger.info("Fant rapportert inntekt i perioden [${it.fraOgMed} - ${it.tilOgMed}] for oppgave med referanse $oppgaveDTO: $it") }
            .let {
                val inntektsrapporteringOppgavetypeDataDTO =
                    oppgaveDTO.oppgavetypeData as? InntektsrapporteringOppgavetypeDataDTO ?: error("OppgavetypeDataDTO er ikke av type InntektsrapporteringOppgavetypeDataDTO")

                logger.info("Oppdaterer oppgave med rapportert inntekt for oppgaveReferanse $oppgaveDTO: $it")
                oppgaveDTO.copy(
                    oppgavetypeData = inntektsrapporteringOppgavetypeDataDTO.copy(rapportertInntekt = it)
                )
            }
    }

    private fun Søknad.rapportertInntektPeriodeInfo(): RapportertInntektPeriodeinfoDTO {
        val oppgittInntektForPeriode = getYtelse<Ungdomsytelse>()
            .inntekter
            .oppgittePeriodeinntekter
            .first

        return RapportertInntektPeriodeinfoDTO(
            fraOgMed = oppgittInntektForPeriode.periode.fraOgMed,
            tilOgMed = oppgittInntektForPeriode.periode.tilOgMed,
            arbeidstakerOgFrilansInntekt = oppgittInntektForPeriode.arbeidstakerOgFrilansInntekt,
            inntektFraYtelse = oppgittInntektForPeriode.ytelse
        )
    }
}
