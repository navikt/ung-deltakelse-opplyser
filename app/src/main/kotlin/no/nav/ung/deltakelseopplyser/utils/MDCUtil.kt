package no.nav.ung.deltakelseopplyser.utils

import no.nav.ung.deltakelseopplyser.utils.Constants.CORRELATION_ID
import org.slf4j.MDC
import java.util.*

object MDCUtil {
    private val GEN = CallIdGenerator()
    @JvmStatic
    fun callId(): String? {
        return MDC.get(CORRELATION_ID)
    }

    fun callIdOrNew(): String {
        return Optional.ofNullable(callId()).orElse(GEN.create())
    }

    fun toMDC(key: String?, value: Any?) {
        if (value != null) {
            toMDC(key, value.toString())
        }
    }

    @JvmOverloads
    fun toMDC(key: String?, value: String?, defaultValue: String? = "null") {
        MDC.put(key, Optional.ofNullable(value)
                .orElse(defaultValue))
    }
}
