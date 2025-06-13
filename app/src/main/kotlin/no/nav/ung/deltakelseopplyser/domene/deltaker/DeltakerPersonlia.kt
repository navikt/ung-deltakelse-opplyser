package no.nav.ung.deltakelseopplyser.domene.deltaker

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.pdl.generated.hentperson.Navn
import no.nav.sif.abac.kontrakt.abac.Diskresjonskode
import java.time.LocalDate
import java.util.*

data class DeltakerPersonalia(
    val id: UUID? = null,
    val deltakerIdent: String,
    val navn: Navn,
    val fødselsdato: LocalDate,

    @Schema(description = "Diskresjonskoder som gjelder for deltakeren. Vl være tom hvis deltaker ikke har diskresjonskoder satt.")
    val diskresjonskoder: Set<Diskresjonskode>,

    @Schema(hidden = true)
    private val programOppstartdato: LocalDate? = null,
) {
    @get:JsonProperty("førsteMuligeInnmeldingsdato")
    val førsteMuligeInnmeldingsdato: LocalDate
        get() {
            val aldersDato = fødselsdato.plusYears(18).plusMonths(1)
            return programOppstartdato
                ?.let { maxOf(aldersDato, it) }
                ?: aldersDato
        }

    @get:JsonProperty("sisteMuligeInnmeldingsdato")
    val sisteMuligeInnmeldingsdato: LocalDate
        get() {
            val aldersDatoSiste = fødselsdato.plusYears(29)
            return programOppstartdato
                ?.let { maxOf(aldersDatoSiste, it) }
                ?: aldersDatoSiste
        }
}
