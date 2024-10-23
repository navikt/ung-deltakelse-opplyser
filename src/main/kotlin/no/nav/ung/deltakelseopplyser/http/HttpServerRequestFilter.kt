package no.nav.ung.deltakelseopplyser.http

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.ung.deltakelseopplyser.utils.CallIdGenerator
import no.nav.ung.deltakelseopplyser.utils.Constants
import no.nav.ung.deltakelseopplyser.utils.IgnoredPathUtils
import no.nav.ung.deltakelseopplyser.utils.MDCUtil
import no.nav.ung.deltakelseopplyser.utils.NavHeaders
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.io.IOException

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class HttpServerRequestFilter(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    @Value("\${spring.application.name:k9-brukerdialog-prosessering}") private val applicationName: String,
) : Filter {

    companion object {
        private val logger = LoggerFactory.getLogger(HttpServerRequestFilter::class.java)

        private val generator = CallIdGenerator()
    }

    @Throws(IOException::class, ServletException::class)
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpServletRequest = request as HttpServletRequest
        val jwtToken = tokenValidationContextHolder.getTokenValidationContext().firstValidToken

        putHeadersToMDC(httpServletRequest, jwtToken)

        logRequest(httpServletRequest, jwtToken)

        chain.doFilter(request, response)

        val httpServletResponse = response as HttpServletResponse
        logResponse(request, httpServletResponse)
    }

    private fun logResponse(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val status = response.status
        val method = request.method
        if (!IgnoredPathUtils.isIgnoredPath(request.requestURI)) {
            logger.info("<-- Response $status $method ${request.requestURI}")
        }
    }

    private fun logRequest(request: HttpServletRequest, jwtToken: JwtToken?) {
        val reqMethod = request.method
        val requestURI = request.requestURI
        val issuer = jwtToken?.let { "[${jwtToken.issuer}]" } ?: ""
        val requestMessage = "--> Request $reqMethod $requestURI $issuer"

        if (!IgnoredPathUtils.isIgnoredPath(requestURI)) {
            logger.info(requestMessage)
        }
    }

    private fun putHeadersToMDC(req: HttpServletRequest, jwtToken: JwtToken?) {
        try {
            MDCUtil.toMDC(Constants.NAV_CONSUMER_ID, req.getHeader(Constants.NAV_CONSUMER_ID), applicationName)
            MDCUtil.toMDC(Constants.CORRELATION_ID, req.getHeader(NavHeaders.X_CORRELATION_ID), generator.create())
            MDCUtil.toMDC(Constants.CALLER_CLIENT_ID, jwtToken?.jwtClaimsSet?.getClaim("client_id"))
        } catch (e: Exception) {
            logger.warn("Feil ved setting av MDC-verdier for {}, MDC-verdier er inkomplette", req.requestURI, e)
        }
    }

    override fun toString(): String {
        return javaClass.simpleName + " [generator=" + generator + ", applicationName=" + applicationName + "]"
    }
}
