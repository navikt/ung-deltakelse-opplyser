package no.nav.ung.deltakelseopplyser.oppgave

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

@Entity(name = "oppgave")
data class OppgaveDAO(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID,

    // Her definerer vi kun oppgavetype og status.
    // Kolonnen "deltakelse_id" vil settes automatisk via relasjonen i UngdomsprogramDeltakelseDAO.

    @Enumerated(EnumType.STRING)
    @Column(name = "oppgavetype", nullable = false)
    val oppgavetype: OppgaveType,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: OppgaveStatus,

    @Column(name = "opprettet_dato", nullable = false)
    val opprettetDato: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),

    @Column(name = "løst_dato")
    val løstDato: ZonedDateTime? = null,
) {
    override fun toString(): String {
        return "OppgaveDAO(id=$id, oppgavetype=$oppgavetype, status=$status, opprettetDato=$opprettetDato, losDato=$løstDato)"
    }
}

enum class OppgaveType {
    BEKREFT_ENDRET_STARTDATO,
    BEKREFT_ENDRET_SLUTTDATO,
}

enum class OppgaveStatus {
    LØST,
    ULØST
}
