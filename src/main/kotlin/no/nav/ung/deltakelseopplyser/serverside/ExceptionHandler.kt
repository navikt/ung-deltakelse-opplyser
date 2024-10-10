package no.nav.ung.deltakelseopplyser.serverside

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class ExceptionHandler : ResponseEntityExceptionHandler() {

    companion object {
        private val log: Logger =
            LoggerFactory.getLogger(ExceptionHandler::class.java)
    }
}
