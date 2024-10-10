package no.nav.ung.deltakelseopplyser.register

import no.nav.ung.deltakelseopplyser.validation.ParameterType
import no.nav.ung.deltakelseopplyser.validation.ValidationErrorResponseException
import no.nav.ung.deltakelseopplyser.validation.ValidationProblemDetails
import no.nav.ung.deltakelseopplyser.validation.Violation
import java.time.LocalDate
import java.util.*

data class DeltakelseOpplysningDTO(
    val id: UUID? = null,
    val deltakerIdent: String,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate? = null,
) {
    fun verifiserIkkeOverlapper(eksisterendeDeltakelser: List<DeltakelseOpplysningDTO>) {
        val sorterteDeltakelser = eksisterendeDeltakelser.sortedWith(compareByDescending { it.fraOgMed.dayOfWeek })

        sorterteDeltakelser.forEach { eksisterendeDeltakelse ->
            val nyFra = this.fraOgMed
            val nyTil = this.tilOgMed ?: nyFra.plusYears(1)
            val eksisterendeFra = eksisterendeDeltakelse.fraOgMed
            val eksisterendeTil = eksisterendeDeltakelse.tilOgMed ?: eksisterendeFra.plusYears(1)

            if (!(nyTil.isBefore(eksisterendeFra) || nyFra.isAfter(eksisterendeTil))) {
                val feilmelding = "[$nyFra - $nyTil] overlapper med [$eksisterendeFra - $eksisterendeTil]"
                throw ValidationErrorResponseException(
                    ValidationProblemDetails(
                        setOf(
                            Violation(
                                parameterName = "deltakelseOpplysningDTO.fraOgMed",
                                parameterType = ParameterType.ENTITY,
                                reason = feilmelding,
                                invalidValue = fraOgMed
                            )
                        )
                    )
                )
            }
        }
    }

    override fun toString(): String {
        return "DeltakerProgramOpplysningDTO(id=$id, deltakerIdentSatt='${deltakerIdent.isNotBlank()}', fraOgMed=$fraOgMed, tilOgMed=$tilOgMed)"
    }
}
