package no.nav.ung.deltakelseopplyser.statistikk.deltaker

interface AntallDeltakerePerOppgavetype {
    fun getOppgavetype(): String
    fun getAntallDeltakere(): Long
}
