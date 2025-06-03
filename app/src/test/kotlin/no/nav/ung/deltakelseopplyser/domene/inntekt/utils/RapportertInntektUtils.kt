package no.nav.ung.deltakelseopplyser.domene.inntekt.utils

import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Kildesystem
import no.nav.k9.søknad.felles.Versjon
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Språk
import no.nav.k9.søknad.ytelse.ung.v1.UngSøknadstype
import no.nav.k9.søknad.ytelse.ung.v1.Ungdomsytelse
import no.nav.k9.søknad.ytelse.ung.v1.inntekt.OppgittInntekt
import java.time.ZonedDateTime
import java.util.*

object RapportertInntektUtils {
    fun lagInntektsrapporteringsSøknad(
        oppgaveReferanse: UUID,
        deltakerIdent: String,
        oppgittInntekt: OppgittInntekt,
    ) = Søknad()
        .medSøknadId(oppgaveReferanse.toString())
        .medMottattDato(ZonedDateTime.now())
        .medSpråk(Språk.NORSK_BOKMÅL)
        .medKildesystem(Kildesystem.SØKNADSDIALOG)
        .medVersjon(Versjon.of("1.0.0"))
        .medSøker(Søker(NorskIdentitetsnummer.of(deltakerIdent)))
        .medYtelse(
            Ungdomsytelse()
                .medSøknadType(UngSøknadstype.RAPPORTERING_SØKNAD)
                .medInntekter(oppgittInntekt)
        )
}
