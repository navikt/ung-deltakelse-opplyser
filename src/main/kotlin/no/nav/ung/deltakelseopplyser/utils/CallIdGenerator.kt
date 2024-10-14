package no.nav.ung.deltakelseopplyser.utils

import java.util.*

class CallIdGenerator {
    fun create(): String {
        return UUID.randomUUID().toString()
    }
}
