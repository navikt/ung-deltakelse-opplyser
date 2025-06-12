package no.nav.ung.deltakelseopplyser.utils

import com.nimbusds.jwt.SignedJWT
import io.mockk.every
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.ung.deltakelseopplyser.config.Issuers.TOKEN_X

object TokenTestUtils {
    const val MOCK_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaXNzIjoidG9rZW54IiwiaWF0IjoxNTE2MjM5MDIyfQ.LVLy4KvnogeEqfc3V0AYHoNDATNkqqUSzQS43zhgYeo"

    fun MockOAuth2Server.hentToken(
        subject: String = FødselsnummerGenerator.neste(),
        issuerId: String = TOKEN_X,
        claims: Map<String, String> = mapOf("acr" to "Level4"),
        audience: String = "aud-localhost",
        expiry: Long = 3600,
    ): SignedJWT =
        issueToken(issuerId = issuerId, subject = subject, claims = claims, audience = audience, expiry = expiry)

    fun SpringTokenValidationContextHolder.mockContext(encodedToken: String = MOCK_TOKEN) {
        every { getTokenValidationContext() } returns TokenValidationContext(mapOf(FødselsnummerGenerator.neste() to JwtToken(encodedToken)))
    }
}
