package no.nav.ung.deltakelseopplyser

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
	fromApplication<UngDeltakelseOpplyserApplication>().with(TestcontainersConfiguration::class).run(*args)
}
