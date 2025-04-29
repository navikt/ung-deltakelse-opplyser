package no.nav.ung.deltakelseopplyser

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.retry.annotation.EnableRetry
import org.springframework.transaction.annotation.EnableTransactionManagement

@EnableTransactionManagement
@SpringBootApplication
@EnableRetry
class UngDeltakelseOpplyserApplication

fun main(args: Array<String>) {
    runApplication<UngDeltakelseOpplyserApplication>(*args)
}
