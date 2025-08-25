package no.nav.ung.deltakelseopplyser.domene.register.historikk

import java.time.ZonedDateTime

data class SøktTidspunktHistorikk(
    val søktTidspunktSatt: Boolean,
    val søktTidspunkt: ZonedDateTime
)
