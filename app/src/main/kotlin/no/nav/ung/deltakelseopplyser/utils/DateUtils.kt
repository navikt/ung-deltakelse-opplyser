package no.nav.ung.deltakelseopplyser.utils

import java.time.LocalDate
import java.time.Month

object DateUtils {

    fun LocalDate.måned(): String = when (month) {
        Month.JANUARY -> "januar"
        Month.FEBRUARY -> "februar"
        Month.MARCH -> "mars"
        Month.APRIL -> "april"
        Month.MAY -> "mai"
        Month.JUNE -> "juni"
        Month.JULY -> "juli"
        Month.AUGUST -> "august"
        Month.SEPTEMBER -> "september"
        Month.OCTOBER -> "oktober"
        Month.NOVEMBER -> "november"
        Month.DECEMBER -> "desember"
    }
}
