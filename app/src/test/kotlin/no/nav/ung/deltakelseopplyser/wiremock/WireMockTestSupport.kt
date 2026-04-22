package no.nav.ung.deltakelseopplyser.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener
import org.springframework.test.context.TestExecutionListeners

/**
 * Erstatter `@org.springframework.cloud.contract.wiremock.AutoConfigureWireMock` som ble
 * fjernet i Spring Cloud Contract 5.0.
 *
 * Starter en delt `WireMockServer` på tilfeldig port, registrerer den som singleton-bean
 * `wireMockServer` og eksponerer porten via property `wiremock.server.port`.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ContextConfiguration(initializers = [WireMockContextInitializer::class])
@TestExecutionListeners(
    listeners = [WireMockResetTestExecutionListener::class],
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS,
)
annotation class AutoConfigureWireMock

internal object WireMockHolder {
    val server: WireMockServer by lazy {
        WireMockServer(wireMockConfig().dynamicPort()).also {
            it.start()
            Runtime.getRuntime().addShutdownHook(Thread { it.stop() })
        }
    }
}

class WireMockContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        val server = WireMockHolder.server
        val beanFactory = applicationContext.beanFactory
        if (!beanFactory.containsBean("wireMockServer")) {
            beanFactory.registerSingleton("wireMockServer", server)
        }
        TestPropertyValues.of("wiremock.server.port=${server.port()}").applyTo(applicationContext)
    }
}

class WireMockResetTestExecutionListener : TestExecutionListener {
    override fun beforeTestMethod(testContext: TestContext) {
        if (testContext.applicationContext.containsBean("wireMockServer")) {
            WireMockHolder.server.resetAll()
        }
    }
}
