package no.nav.ung.deltakelseopplyser.domene.deltaker

import no.nav.pdl.generated.hentperson.Foedselsdato
import no.nav.pdl.generated.hentperson.Folkeregisteridentifikator
import no.nav.pdl.generated.hentperson.Navn
import no.nav.pdl.generated.hentperson.Person
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object Scenarioer {

    fun lagPerson(fødselsdato: LocalDate): Person {
        return Person(
            navn = listOf(
                Navn(
                    fornavn = "Ola",
                    etternavn = "Nordmann"
                )
            ),
            foedselsdato = listOf(
                Foedselsdato(fødselsdato.format(DateTimeFormatter.ISO_LOCAL_DATE))
            ),
            folkeregisteridentifikator = listOf(
                Folkeregisteridentifikator("12345678901")
            )
        )
    }

}