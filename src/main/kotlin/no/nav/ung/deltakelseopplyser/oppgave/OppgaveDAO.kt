package no.nav.ung.deltakelseopplyser.oppgave

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import no.nav.ung.deltakelseopplyser.register.UngdomsprogramDeltakelseDAO
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.Type
import org.hibernate.type.SqlTypes
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

@Entity(name = "oppgave")
class OppgaveDAO(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deltakelse_id", nullable = false)
    val deltakelse: UngdomsprogramDeltakelseDAO,

    @Enumerated(EnumType.STRING)
    @Column(name = "oppgavetype", nullable = false)
    val oppgavetype: Oppgavetype,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: OppgaveStatus,

    /**
     * JSON-kolonne for oppgavetype-spesifikk data.
     */
    @Type(JsonBinaryType::class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "oppgavetype_data", columnDefinition = "jsonb")
    val oppgavetypeDataDAO: OppgavetypeDataDAO,

    @Column(name = "opprettet_dato", nullable = false)
    val opprettetDato: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),

    @Column(name = "løst_dato")
    val løstDato: ZonedDateTime? = null,
) {
    override fun toString(): String {
        return "OppgaveDAO(id=$id, oppgavetype=$oppgavetype, status=$status, opprettetDato=$opprettetDato, losDato=$løstDato)"
    }
}

enum class Oppgavetype {
    BEKREFT_ENDRET_STARTDATO,
    BEKREFT_ENDRET_SLUTTDATO,
}

enum class OppgaveStatus {
    LØST,
    ULØST
}
