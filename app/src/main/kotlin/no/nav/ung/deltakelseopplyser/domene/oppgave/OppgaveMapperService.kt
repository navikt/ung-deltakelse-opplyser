package no.nav.ung.deltakelseopplyser.domene.oppgave

import no.nav.ung.deltakelseopplyser.domene.inntekt.RapportertInntektService
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.EndretSluttdatoOppgaveDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.EndretStartdatoOppgaveDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.FjernetPeriodeOppgaveDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.InntektsrapporteringOppgavetypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.KontrollerRegisterInntektOppgaveTypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveBekreftelse
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgavetypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.RegisterinntektDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.SøkYtelseOppgavetypeDataDAO
import no.nav.ung.deltakelseopplyser.integration.enhetsregisteret.EnhetsregisterService
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.ArbeidOgFrilansRegisterInntektDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.BekreftelseDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.EndretSluttdatoDataDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.EndretStartdatoDataDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.FjernetPeriodeDataDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.InntektsrapporteringOppgavetypeDataDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.KontrollerRegisterinntektOppgavetypeDataDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveStatus
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgavetypeDataDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.RegisterinntektDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.SøkYtelseOppgavetypeDataDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.YtelseRegisterInntektDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.FjernetPeriodeOppgaveDTO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OppgaveMapperService(
    private val enhetsregisterService: EnhetsregisterService,
    private val rapportertInntektService: RapportertInntektService,
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(OppgaveMapperService::class.java)
    }

    fun mapOppgaveTilDTO(oppgaveDAO: OppgaveDAO): OppgaveDTO {
        return OppgaveDTO(
            oppgaveReferanse = oppgaveDAO.oppgaveReferanse,
            oppgavetype = oppgaveDAO.oppgavetype,
            oppgavetypeData = oppgaveDAO.oppgavetypeDataDAO.tilDTO(),
            bekreftelse = oppgaveDAO.oppgaveBekreftelse?.tilDTO(),
            status = oppgaveDAO.status,
            opprettetDato = oppgaveDAO.opprettetDato,
            løstDato = oppgaveDAO.løstDato,
            åpnetDato = oppgaveDAO.åpnetDato,
            lukketDato = oppgaveDAO.lukketDato,
            frist = oppgaveDAO.frist
        ).let { oppgaveDTO ->
            if (oppgaveDAO.erLøstInntektsrapportering()) {
                rapportertInntektService.leggPåRapportertInntekt(oppgaveDTO)
            } else oppgaveDTO
        }
    }

    fun OppgaveBekreftelse.tilDTO(): BekreftelseDTO = BekreftelseDTO(
        harUttalelse = harUttalelse,
        uttalelseFraBruker = uttalelseFraBruker,
    )

    fun OppgavetypeDataDAO.tilDTO(): OppgavetypeDataDTO = when (this) {
        is EndretStartdatoOppgaveDataDAO -> EndretStartdatoDataDTO(
            nyStartdato = this.nyStartdato,
            forrigeStartdato = this.forrigeStartdato,
        )

        is EndretSluttdatoOppgaveDataDAO -> EndretSluttdatoDataDTO(
            nySluttdato = this.nySluttdato,
            forrigeSluttdato = this.forrigeSluttdato,
        )

        is FjernetPeriodeOppgaveDataDAO -> FjernetPeriodeDataDTO(
            forrigeStartdato = this.forrigeStartdato,
            forrigeSluttdato = this.forrigeSluttdato
        )

        is KontrollerRegisterInntektOppgaveTypeDataDAO -> KontrollerRegisterinntektOppgavetypeDataDTO(
            fomDato,
            tomDato,
            registerinntekt.tilDTO(),
            gjelderSisteMåned
        )

        is InntektsrapporteringOppgavetypeDataDAO -> {
            InntektsrapporteringOppgavetypeDataDTO(
                fraOgMed = this.fomDato,
                tilOgMed = this.tomDato,
                rapportertInntekt = null
            )
        }

        is SøkYtelseOppgavetypeDataDAO -> SøkYtelseOppgavetypeDataDTO(
            fomDato = fomDato
        )
    }

    fun RegisterinntektDAO.tilDTO() = RegisterinntektDTO(
        arbeidOgFrilansInntekter = arbeidOgFrilansInntekter.map {
            ArbeidOgFrilansRegisterInntektDTO(
                it.inntekt,
                it.arbeidsgiver,
                hentArbeidsgiverNavn(it.arbeidsgiver)
            )
        },
        ytelseInntekter = ytelseInntekter.map { YtelseRegisterInntektDTO(it.inntekt, it.ytelsetype) }
    )

    private fun String.erOrganisasjonsnummer() = length == 9 && all { it.isDigit() }

    private fun hentArbeidsgiverNavn(arbeidsgiver: String): String? {
        return if (!arbeidsgiver.erOrganisasjonsnummer()) {
            logger.info("Arbeidsgiver er ikke organisasjonsnummer. Returnerer null.")
            null
        } else {
            kotlin.runCatching { enhetsregisterService.hentOrganisasjonsinfo(arbeidsgiver) }
                .fold(
                    onSuccess = {
                        val sammensattnavn = it.navn?.sammensattnavn
                        if (sammensattnavn.isNullOrBlank()) {
                            logger.warn("Organisasjonsnummer $arbeidsgiver hadde ikke navn i Enhetsregisteret. Returnerer null.")
                            null
                        } else
                            sammensattnavn
                    },
                    onFailure = {
                        logger.warn("Kunne ikke hente organisasjonsnavn for $arbeidsgiver. Returnerer null. ${it.message}.")
                        null
                    }
                )
        }
    }

    private fun OppgaveDAO.erLøstInntektsrapportering() =
        this.oppgavetype == Oppgavetype.RAPPORTER_INNTEKT && this.status == OppgaveStatus.LØST

}
