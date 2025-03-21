package no.nav.ung.deltakelseopplyser.domene.register.ungsak

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class RegisterInntektOppgaveDTO(
    val aktørId: String,
    val referanse: UUID,
    val frist: LocalDateTime,
    val fomDato: LocalDate,
    val tomDato: LocalDate,
    val registerInntekter: RegisterInntektDTO,
    )

data class RegisterInntektDTO(
    val registerinntekterForArbeidOgFrilans: List<RegisterInntektArbeidOgFrilansDTO>? = listOf(),
    val registerinntekterForYtelse: List<RegisterInntektYtelseDTO>? = listOf(),
)

data class RegisterInntektYtelseDTO (
    val beløp: Int,
    val ytelseType: String,
)

data class RegisterInntektArbeidOgFrilansDTO (
    val beløp: Int,
    val arbeidsgiverIdent: String,
)

