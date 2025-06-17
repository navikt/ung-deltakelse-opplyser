package no.nav.ung.deltakelseopplyser.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class DeltakerappConfig(
    @Value("\${UNGDOMSYTELSE_DELTAKER_BASE_URL}") private val deltakerAppBaseUrl: String
) {

    fun getOppgaveUrl(oppgaveId: String): String {
        return "$deltakerAppBaseUrl/oppgave/$oppgaveId"
    }

    fun getSøknadUrl(): String {
        return "$deltakerAppBaseUrl"
    }
}
