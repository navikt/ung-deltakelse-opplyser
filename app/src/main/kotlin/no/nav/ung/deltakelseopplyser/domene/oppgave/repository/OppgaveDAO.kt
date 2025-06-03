package no.nav.ung.deltakelseopplyser.domene.oppgave.repository

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import jakarta.persistence.*
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDAO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.*
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

    @Column(name = "frist", nullable = true)
    val frist: ZonedDateTime? = null,

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

    companion object {
        fun OppgaveDAO.tilDTO() = OppgaveDTO(
            oppgaveReferanse = oppgaveReferanse,
            oppgavetype = oppgavetype,
            oppgavetypeData = oppgavetypeDataDAO.tilDTO(),
            bekreftelse = oppgaveBekreftelse?.tilDTO(),
            status = status,
            opprettetDato = opprettetDato,
            løstDato = løstDato,
            åpnetDato = åpnetDato,
            lukketDato = lukketDato,
            frist = frist
        )

        fun OppgaveBekreftelse.tilDTO(): BekreftelseDTO = BekreftelseDTO(
            harGodtattEndringen = harGodtattEndringen,
            uttalelseFraBruker = uttalelseFraBruker,
        )

        fun OppgavetypeDataDAO.tilDTO(): OppgavetypeDataDTO = when (this) {
            is EndretStartdatoOppgaveDataDAO -> EndretStartdatoDataDTO(
                nyStartdato = this.nyStartdato,
                forrigeStartdato = this.forrigeStartdato,
            )

            is EndretSluttdatoOppgaveDataDAO -> EndretSluttdatoDataDTO(
                nySluttdato = this.nySluttdato,
                forrigeSluttdato = this.forrigeSluttdato,
            )

            is KontrollerRegisterInntektOppgaveTypeDataDAO -> KontrollerRegisterinntektOppgavetypeDataDTO(
                fomDato,
                tomDato,
                registerinntekt.tilDTO()
            )

            is InntektsrapporteringOppgavetypeDataDAO -> {
                InntektsrapporteringOppgavetypeDataDTO(
                    fraOgMed = this.fomDato,
                    tilOgMed = this.tomDato,
                    rapportertInntekt = null
                )
            }

            is SøkYtelseOppgavetypeDataDAO -> SøkYtelseOppgavetypeDataDTO(
                fomDato = fomDato
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
        this.løstDato = ZonedDateTime.now(ZoneOffset.UTC)
        return this
    }
}
