package no.nav.ung.deltakelseopplyser.validation

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.ErrorResponseException
import java.net.URI

data class ValidationProblemDetails(val violations: Set<Violation>): ProblemDetail(400) {
    init {
        type = URI("/problem-details/invalid-request-parameters")
        title = "invalid-request-parameters"
        detail = "Forespørselen inneholder valideringsfeil"
    }
}

class ValidationErrorResponseException(val validationProblemDetails: ProblemDetail) : ErrorResponseException(HttpStatus.BAD_REQUEST, validationProblemDetails, null)

data class Violation(
    val parameterName: String,
    val parameterType: ParameterType,
    val reason: String,
    val invalidValue: Any? = null,
)

enum class ParameterType {
    QUERY,
    PATH,
    HEADER,
    ENTITY,
    FORM
}
