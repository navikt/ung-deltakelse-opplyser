package no.nav.ung.deltakelseopplyser.utils

import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter

object DateUtils {
    val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")

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
