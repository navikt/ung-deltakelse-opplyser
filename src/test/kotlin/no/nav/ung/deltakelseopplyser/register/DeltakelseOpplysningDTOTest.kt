package no.nav.ung.deltakelseopplyser.register

import no.nav.ung.deltakelseopplyser.validation.ValidationErrorResponseException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class DeltakelseOpplysningDTOTest {

    @Test
    fun `Registrering av ny deltakelse med fom dato før eksisterende deltakelsesperiode uten tom dato feiler`() {
        val eksisterendeDeltakelser = listOf(
            DeltakelseOpplysningDTO(
                deltakerIdent = "123",
                fraOgMed = LocalDate.parse("2024-09-07"),
                tilOgMed = LocalDate.parse("2024-09-09")
            ),
            DeltakelseOpplysningDTO(
                deltakerIdent = "123",
                fraOgMed = LocalDate.parse("2024-10-07"),
                tilOgMed = null
            )
        )

        val exception: ValidationErrorResponseException = assertThrows<ValidationErrorResponseException> {
            DeltakelseOpplysningDTO(
                deltakerIdent = "123",
                fraOgMed = LocalDate.parse("2024-09-10"),
                tilOgMed = null
            ).verifiserIkkeOverlapper(eksisterendeDeltakelser)
        }
        assertThat(exception.message).contains("Ny periode[2024-09-10 - 2025-09-10] overlapper med eksisterende periode[2024-10-07 - 2025-10-07]")
        println(exception)
    }

    @Test
    fun `Registrering av ny deltakelse med fom dato midt i en eksisterende deltakelsesperiode feiler`() {
        val eksisterendeDeltakelser = listOf(
            DeltakelseOpplysningDTO(
                deltakerIdent = "123",
                fraOgMed = LocalDate.parse("2024-09-07"),
                tilOgMed = LocalDate.parse("2024-09-09")
            ),
            DeltakelseOpplysningDTO(
                deltakerIdent = "123",
                fraOgMed = LocalDate.parse("2024-10-07"),
                tilOgMed = null
            )
        )

        val exception: ValidationErrorResponseException = assertThrows<ValidationErrorResponseException> {
            DeltakelseOpplysningDTO(
                deltakerIdent = "123",
                fraOgMed = LocalDate.parse("2024-09-08"),
                tilOgMed = null
            ).verifiserIkkeOverlapper(eksisterendeDeltakelser)
        }
        assertThat(exception.message).contains("Ny periode[2024-09-08 - 2025-09-08] overlapper med eksisterende periode[2024-09-07 - 2024-09-09]")
        println(exception)
    }

    @Test
    fun `Registrering av ny deltakelse med fom dato etter en eksisterende deltakelsesperiode uten fom dato feiler`() {
        val eksisterendeDeltakelser = listOf(
            DeltakelseOpplysningDTO(
                deltakerIdent = "123",
                fraOgMed = LocalDate.parse("2024-09-07"),
                tilOgMed = LocalDate.parse("2024-09-09")
            ),
            DeltakelseOpplysningDTO(
                deltakerIdent = "123",
                fraOgMed = LocalDate.parse("2024-10-07"),
                tilOgMed = null
            )
        )

        val exception: ValidationErrorResponseException = assertThrows<ValidationErrorResponseException> {
            DeltakelseOpplysningDTO(
                deltakerIdent = "123",
                fraOgMed = LocalDate.parse("2024-10-10"),
                tilOgMed = null
            ).verifiserIkkeOverlapper(eksisterendeDeltakelser)
        }
        assertThat(exception.message).contains("Ny periode[2024-10-10 - 2025-10-10] overlapper med eksisterende periode[2024-10-07 - 2025-10-07]")
        println(exception)
    }
}
