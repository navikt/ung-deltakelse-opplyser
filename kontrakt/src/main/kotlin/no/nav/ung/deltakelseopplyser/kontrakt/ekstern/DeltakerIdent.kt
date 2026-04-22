package no.nav.ung.deltakelseopplyser.kontrakt.ekstern

import com.fasterxml.jackson.annotation.JsonProperty

data class DeltakerIdent(@JsonProperty("deltakerIdent") val ident: String)
