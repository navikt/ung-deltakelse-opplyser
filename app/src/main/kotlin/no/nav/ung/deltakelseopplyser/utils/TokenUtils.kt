package no.nav.ung.deltakelseopplyser.utils

import no.nav.security.token.support.spring.SpringTokenValidationContextHolder

object TokenClaims {
    // Brukerident ligger i pid claim på tokenet for flyten idporten -> tokenx
    const val CLAIM_PID = "pid"

    // Brukerident ligger i sub claim på tokenet for flyten NAV loginservice -> tokenx
    const val CLAIM_SUB = "sub"

    // NAV ident ligger i navident claim på tokenet flyten azure.
    const val NAV_IDENT = "NAVident"
}

fun SpringTokenValidationContextHolder.personIdent(): String {
    val jwtToken = getTokenValidationContext().firstValidToken
        ?: throw IllegalStateException("Ingen gyldige tokens i Authorization headeren")

    val pid = jwtToken.jwtTokenClaims.getStringClaim(TokenClaims.CLAIM_PID)
    val sub = jwtToken.jwtTokenClaims.getStringClaim(TokenClaims.CLAIM_SUB)

    return when {
        !pid.isNullOrBlank() -> pid
        !sub.isNullOrBlank() -> sub
        else -> throw IllegalStateException("Ugyldig token. Token inneholdt verken sub eller pid claim")
    }
}

fun SpringTokenValidationContextHolder.subject(): String {
    val jwtToken = getTokenValidationContext().firstValidToken
        ?: throw IllegalStateException("Ingen gyldige tokens i Authorization headeren")

    val sub = jwtToken.jwtTokenClaims.getStringClaim(TokenClaims.CLAIM_SUB)
    return when {
        !sub.isNullOrBlank() -> sub
        else -> throw IllegalStateException("Ugyldig token. Token inneholdt verken sub eller pid claim")
    }
}

fun SpringTokenValidationContextHolder.navIdent(): String {
    val jwtToken = getTokenValidationContext().firstValidToken
        ?: throw IllegalStateException("Ingen gyldige tokens i Authorization headeren")

    val navIdent = jwtToken.jwtTokenClaims.getStringClaim(TokenClaims.NAV_IDENT)
    return when {
        !navIdent.isNullOrBlank() -> navIdent
        else -> throw IllegalStateException("Ugyldig token. Token inneholdt verken sub eller pid claim")
    }
}


