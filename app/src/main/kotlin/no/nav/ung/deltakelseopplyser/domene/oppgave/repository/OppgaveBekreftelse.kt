package no.nav.ung.deltakelseopplyser.domene.oppgave.repository

import java.io.Serializable

class OppgaveBekreftelse(
    val harUttalelse: Boolean,
    val uttalelseFraBruker: String? = null,
) : Serializable
