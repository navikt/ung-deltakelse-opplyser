package no.nav.ung.deltakelseopplyser

import org.junit.jupiter.api.Test

class UngDeltakelseOpplyserApplicationTests : AbstractIntegrationTest() {

    @Test
    fun contextLoads() {
    }

    override val consumerGroupPrefix: String
        get() = "UngDeltakelseOpplyserApplicationTests"
    override val consumerGroupTopics: List<String>
        get() = listOf()
}
