package no.nav.ung.deltakelseopplyser.utils

import java.time.LocalDate

object FødselsnummerGenerator {

    private val random = kotlin.random.Random(System.currentTimeMillis())

    fun neste(): String {
        //genererer noe som ser ut som fødselsnummer, men oppfyller ikke kontrollsiffer-regler
        val fødselsdato = LocalDate.ofYearDay(random.nextInt(1900, 2000), random.nextInt(1, 366))
        val personnummer = random.nextInt(0, 100000);
        return String.format("%02d%02d%02d%05d", fødselsdato.dayOfMonth, fødselsdato.monthValue, fødselsdato.year % 100, personnummer);
    }
}