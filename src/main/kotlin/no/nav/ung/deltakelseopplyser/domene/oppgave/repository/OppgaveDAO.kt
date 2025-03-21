package no.nav.ung.deltakelseopplyser.domene.oppgave.repository

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramDeltakelseDAO
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

    @Id
    @Column(name = "ekstern_ref", nullable = false)
    val eksternReferanse: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deltakelse_id", nullable = false)
    val deltakelse: UngdomsprogramDeltakelseDAO,

    @Enumerated(EnumType.STRING)
    @Column(name = "oppgavetype", nullable = false)
    val oppgavetype: Oppgavetype,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: OppgaveStatus,

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
    var løstDato: ZonedDateTime? = null,
) {
    override fun toString(): String {
        return "OppgaveDAO(id=$id, oppgavetype=$oppgavetype, status=$status, opprettetDato=$opprettetDato, losDato=$løstDato)"
    }

    fun markerSomLøst(): OppgaveDAO {
        this.status = OppgaveStatus.LØST
        this.løstDato = ZonedDateTime.now(ZoneOffset.UTC)
        return this
    }

    fun markerSomKansellert(): OppgaveDAO {
        this.status = OppgaveStatus.KANSELLERT
        this.løstDato = ZonedDateTime.now(ZoneOffset.UTC)
        return this
    }
}

enum class Oppgavetype {
    BEKREFT_ENDRET_STARTDATO,
    BEKREFT_ENDRET_SLUTTDATO,
    BEKREFT_AVVIK_REGISTERINNTEKT,
}

enum class OppgaveStatus {
    LØST,
    ULØST,
    KANSELLERT
}
