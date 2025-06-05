package no.nav.ung.deltakelseopplyser.domene.register.historikk

import java.time.ZonedDateTime

data class SøktTidspunktHistorikkDTO(
    val søktTidspunktSatt: Boolean,
    val søktTidspunkt: ZonedDateTime
)
