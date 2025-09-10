package no.nav.ung.deltakelseopplyser.domene.deltaker

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.pdl.generated.hentperson.Navn
import no.nav.sif.abac.kontrakt.abac.Diskresjonskode
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

data class DeltakerPersonalia(
    val id: UUID? = null,
    val deltakerIdent: String,
    val navn: Navn,
    val fødselsdato: LocalDate,

    @Schema(description = "Diskresjonskoder som gjelder for deltakeren. Vil være tom hvis deltaker ikke har diskresjonskoder satt.")
    val diskresjonskoder: Set<Diskresjonskode>,

    @Schema(hidden = true)
    private val programOppstartdato: LocalDate? = null,
) {

    companion object {
        private val log = LoggerFactory.getLogger(DeltakerPersonalia::class.java)
    }
    init {
        if (førsteMuligeInnmeldingsdato.isAfter(sisteMuligeInnmeldingsdato)) {
            log.warn("Første innmeldingsdato=$førsteMuligeInnmeldingsdato er satt til siste mulige programdato=$sisteMuligeInnmeldingsdato ")
        }
    }

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
            return fødselsdato.plusYears(29).minusDays(1)
        }
}
