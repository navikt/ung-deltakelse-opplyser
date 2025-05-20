package no.nav.ung.deltakelseopplyser.domene.inntekt

import no.nav.k9.søknad.JsonUtils
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.ytelse.ung.v1.Ungdomsytelse
import no.nav.ung.deltakelseopplyser.domene.inntekt.repository.RapportertInntektRepository
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.RapportertInntektPeriodeinfoDTO
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.stereotype.Service
import org.springframework.web.ErrorResponseException
import java.util.*

@Service
class RapportertInntektService(
    private val rapportertInntektRepository: RapportertInntektRepository,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RapportertInntektService::class.java)
    }

    fun finnRapportertInntektGittOppgaveReferanse(oppgaveReferanse: UUID): RapportertInntektPeriodeinfoDTO? {
        return rapportertInntektRepository
            .finnRapportertInntektGittOppgaveReferanse(oppgaveReferanse.toString())
            ?.let { ungRapportertInntektDAO ->
                JsonUtils.fromString(ungRapportertInntektDAO.inntekt, Søknad::class.java)
            }?.rapportertInntektPeriodeInfo()
            ?.also { logger.info("Fant rapportert inntekt i perioden [${it.fraOgMed} - ${it.tilOgMed}] for oppgave med referanse $oppgaveReferanse: $it") }
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
