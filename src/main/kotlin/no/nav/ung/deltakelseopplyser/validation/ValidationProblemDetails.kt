package no.nav.ung.deltakelseopplyser.validation

import org.springframework.http.ProblemDetail
import java.net.URI

data class ValidationProblemDetails(val violations: Set<Violation>): ProblemDetail(400) {
    init {
        type = URI("/problem-details/invalid-request-parameters")
        title = "invalid-request-parameters"
        detail = "Forespørselen inneholder valideringsfeil"
    }
}

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
