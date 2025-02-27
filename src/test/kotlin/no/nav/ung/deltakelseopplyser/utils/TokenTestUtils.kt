package no.nav.ung.deltakelseopplyser.utils

import com.nimbusds.jwt.SignedJWT
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.ung.deltakelseopplyser.config.Issuers.TOKEN_X

object TokenTestUtils {
    /*
      Header:
      {
        "alg": "HS256",
        "typ": "JWT"
      }

      Payload:
      {
        "sub": "1234567890",
        "name": "John Doe",
        "iat": 1516239022
      }
       */
    const val MOCK_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

    fun MockOAuth2Server.hentToken(
        subject: String = "12345678910",
        issuerId: String = TOKEN_X,
        claims: Map<String, String> = mapOf("acr" to "Level4"),
        audience: String = "aud-localhost",
        expiry: Long = 3600,
    ): SignedJWT = issueToken(issuerId = issuerId, subject = subject, claims = claims, audience = audience, expiry = expiry)

    fun SpringTokenValidationContextHolder.mockContext(encodedToken: String = MOCK_TOKEN) {
        every { getTokenValidationContext() } returns TokenValidationContext(mapOf("1234567890" to JwtToken(encodedToken)))
        every { setTokenValidationContext(any()) } just Runs
    }
}
