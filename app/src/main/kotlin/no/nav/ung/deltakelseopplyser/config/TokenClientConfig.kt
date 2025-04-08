package no.nav.ung.deltakelseopplyser.config

import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import org.springframework.context.annotation.Configuration

@Configuration
@EnableOAuth2Client(cacheEnabled = true, cacheMaximumSize = 1000, cacheEvictSkew = 30)
class TokenClientConfig
