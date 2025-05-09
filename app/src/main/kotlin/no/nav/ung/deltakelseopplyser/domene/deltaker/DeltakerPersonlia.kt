package no.nav.ung.deltakelseopplyser.domene.deltaker

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.pdl.generated.hentperson.Navn
import java.time.LocalDate
import java.util.*

data class DeltakerPersonalia(
    val id: UUID? = null,
    val deltakerIdent: String,
    val navn: Navn,
    val fødselsdato: LocalDate,
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
