package no.nav.ung.deltakelseopplyser

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.resilience.annotation.EnableResilientMethods
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.annotation.EnableTransactionManagement

@EnableTransactionManagement
@SpringBootApplication
@EnableResilientMethods
class UngDeltakelseOpplyserApplication

fun main(args: Array<String>) {
    runApplication<UngDeltakelseOpplyserApplication>(*args)
}
