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
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.ArbeidOgFrilansRegisterInntektDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.EndretSluttdatoOppgavetypeDataDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.EndretStartdatoOppgavetypeDataDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.KontrollerRegisterinntektOppgavetypeDataDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveStatus
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgavetypeDataDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.RegisterinntektDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.YtelseRegisterInntektDTO
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
    @Column(name = "oppgave_referanse", nullable = false)
    val oppgaveReferanse: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deltaker_id", nullable = false)
    val deltaker: DeltakerDAO,

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
    companion object {
        fun OppgaveDAO.tilDTO() = OppgaveDTO(
            oppgaveReferanse = oppgaveReferanse,
            oppgavetype = oppgavetype,
            oppgavetypeData = oppgavetypeDataDAO.tilDTO(),
            status = status,
            opprettetDato = opprettetDato,
            løstDato = løstDato
        )

        fun OppgavetypeDataDAO.tilDTO(): OppgavetypeDataDTO = when (this) {
            is EndretStartdatoOppgavetypeDataDAO -> EndretStartdatoOppgavetypeDataDTO(nyStartdato)

            is EndretSluttdatoOppgavetypeDataDAO -> EndretSluttdatoOppgavetypeDataDTO(nySluttdato)

            is KontrollerRegisterInntektOppgaveTypeDataDAO -> KontrollerRegisterinntektOppgavetypeDataDTO(
                fomDato,
                tomDato,
                registerinntekt.tilDTO()
            )
        }

        fun RegisterinntektDAO.tilDTO() = RegisterinntektDTO(
            arbeidOgFrilansInntekter = arbeidOgFrilansInntekter.map {
                ArbeidOgFrilansRegisterInntektDTO(
                    it.inntekt,
                    it.arbeidsgiver
                )
            },
            ytelseInntekter = ytelseInntekter.map { YtelseRegisterInntektDTO(it.inntekt, it.ytelsetype) }
        )
    }

    override fun toString(): String {
        return "OppgaveDAO(id=$id, oppgavetype=$oppgavetype, status=$status, opprettetDato=$opprettetDato, losDato=$løstDato)"
    }

    fun markerSomLøst(): OppgaveDAO {
        return settStatus(OppgaveStatus.LØST)
    }

    fun markerSomAvbrutt(): OppgaveDAO {
        return settStatus(OppgaveStatus.AVBRUTT)
    }

    fun markerSomUtløpt(): OppgaveDAO {
        return settStatus(OppgaveStatus.UTLØPT)
    }

    fun settStatus(oppgaveStatus: OppgaveStatus): OppgaveDAO {
        this.status = oppgaveStatus
        this.løstDato = ZonedDateTime.now(ZoneOffset.UTC)
        return this
    }
}
