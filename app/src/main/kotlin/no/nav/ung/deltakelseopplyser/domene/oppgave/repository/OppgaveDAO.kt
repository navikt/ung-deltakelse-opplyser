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
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDAO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveStatus
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
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

    @Column(name = "oppgave_referanse", nullable = false)
    val oppgaveReferanse: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deltaker_id", nullable = false)
    val deltaker: DeltakerDAO,

    @Enumerated(EnumType.STRING)
    @Column(name = "oppgavetype", nullable = false)
    val oppgavetype: Oppgavetype,

    @Column(name = "frist", nullable = true)
    var frist: ZonedDateTime? = null,

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

    @Type(JsonBinaryType::class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "oppgave_bekreftelse", columnDefinition = "jsonb")
    var oppgaveBekreftelse: OppgaveBekreftelse? = null,

    @Column(name = "opprettet_dato", nullable = false)
    val opprettetDato: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),

    @Column(name = "løst_dato")
    var løstDato: ZonedDateTime? = null,

    @Column(name = "åpnet_dato")
    var åpnetDato: ZonedDateTime? = null,

    @Column(name = "lukket_dato")
    var lukketDato: ZonedDateTime? = null,
) {

    override fun toString(): String {
        return "OppgaveDAO(id=$id, oppgavetype=$oppgavetype, status=$status, opprettetDato=$opprettetDato, losDato=$løstDato)"
    }

    fun markerSomLøst(): OppgaveDAO {
        this.løstDato = ZonedDateTime.now(ZoneOffset.UTC)
        return settStatus(OppgaveStatus.LØST)
    }

    fun markerSomAvbrutt(): OppgaveDAO {
        return settStatus(OppgaveStatus.AVBRUTT)
    }

    fun markerSomUtløpt(): OppgaveDAO {
        return settStatus(OppgaveStatus.UTLØPT)
    }

    fun markerSomÅpnet(): OppgaveDAO {
        this.åpnetDato = ZonedDateTime.now(ZoneOffset.UTC)
        return this
    }

    fun markerSomLukket(): OppgaveDAO {
        settStatus(OppgaveStatus.LUKKET)
        this.lukketDato = ZonedDateTime.now(ZoneOffset.UTC)
        return this
    }

    fun settStatus(oppgaveStatus: OppgaveStatus): OppgaveDAO {
        this.status = oppgaveStatus
        return this
    }

    fun endreFrist(nyFrist: ZonedDateTime?): OppgaveDAO {
        this.frist = nyFrist
        return this
    }
}
