package no.nav.ung.deltakelseopplyser.domene.deltaker

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.pdl.generated.hentperson.Navn
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

data class DeltakerPersonalia(
    val id: UUID? = null,
    val deltakerIdent: String,
    val navn: Navn,
    val fødselsdato: LocalDate,
) {
    companion object {
        const val PROGRAM_OPPSTART_DATO_PROPERTY = "PROGRAM_OPPSTART_DATO"
    }

    private fun programOppstartDato(): LocalDate? =
        System
            .getProperty(PROGRAM_OPPSTART_DATO_PROPERTY)
            .also {
                if (it != null) {
                    LoggerFactory.getLogger(DeltakerPersonalia::class.java).info("Bruker system-egenskap $PROGRAM_OPPSTART_DATO_PROPERTY med verdi $it")
                }
            }
            .takeIf { it?.isNotBlank() == true }
            ?.let(LocalDate::parse)

    @get:JsonProperty("førsteMuligeInnmeldingsdato")
    val førsteMuligeInnmeldingsdato: LocalDate
        get() {
            val aldersDato = fødselsdato.plusYears(18).plusMonths(1)
            return programOppstartDato()
                ?.let { maxOf(aldersDato, it) }
                ?: aldersDato
        }

    @get:JsonProperty("sisteMuligeInnmeldingsdato")
    val sisteMuligeInnmeldingsdato: LocalDate
        get() {
            val aldersSiste = fødselsdato.plusYears(29)
            return programOppstartDato()
                ?.let { maxOf(aldersSiste, it) }
                ?: aldersSiste
        }
}
