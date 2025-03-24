package no.nav.ung.deltakelseopplyser.utils

import com.nimbusds.jwt.SignedJWT
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.ung.deltakelseopplyser.config.Issuers.TOKEN_X

object TokenTestUtils {
    fun MockOAuth2Server.hentToken(
        subject: String = "12345678910",
        issuerId: String = TOKEN_X,
        claims: Map<String, String> = mapOf("acr" to "Level4"),
        audience: String = "aud-localhost",
        expiry: Long = 3600,
    ): SignedJWT =
        issueToken(issuerId = issuerId, subject = subject, claims = claims, audience = audience, expiry = expiry)
}
