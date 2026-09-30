package no.nav.ung.deltakelseopplyser.domene.register.historikk

import no.nav.ung.deltakelseopplyser.kontrakt.register.Avslutningsårsak
import java.time.LocalDate

data class DeltakerMeldtUtHistorikk(
    val utmeldingDato: LocalDate,
    val avslutningsårsak: Avslutningsårsak? = null,
)
