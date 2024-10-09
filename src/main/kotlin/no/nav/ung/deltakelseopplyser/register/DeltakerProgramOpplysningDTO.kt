package no.nav.ung.deltakelseopplyser.register

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.ErrorResponseException
import java.time.LocalDate
import java.util.*

data class DeltakerProgramOpplysningDTO(
    val id: UUID? = null,
    val deltakerIdent: String,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate? = null,
) {
    fun verifiserIkkeOverlapper(eksisterendeDeltakelser: List<DeltakerProgramOpplysningDTO>) {
        val sorterteDeltakelser = eksisterendeDeltakelser.sortedWith(compareByDescending { it.fraOgMed.dayOfWeek })

        sorterteDeltakelser.forEach { eksisterendeDeltakelse ->
            val nyFra = this.fraOgMed
            val nyTil = this.tilOgMed ?: nyFra.plusYears(1)
            val eksisterendeFra = eksisterendeDeltakelse.fraOgMed
            val eksisterendeTil = eksisterendeDeltakelse.tilOgMed ?: eksisterendeFra.plusYears(1)

            if (!(nyTil.isBefore(eksisterendeFra) || nyFra.isAfter(eksisterendeTil))) {
                val feilmelding = "[$nyFra - $nyTil] overlapper med [$eksisterendeFra - $eksisterendeTil]"
                throw ErrorResponseException(
                    HttpStatus.BAD_REQUEST,
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        feilmelding
                    ),
                    null
                )
            }
        }
    }

    override fun toString(): String {
        return "DeltakerProgramOpplysningDTO(id=$id, deltakerIdentSatt='${deltakerIdent.isNotBlank()}', fraOgMed=$fraOgMed, tilOgMed=$tilOgMed)"
    }
}
