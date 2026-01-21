package no.nav.ung.deltakelseopplyser.domene.register

import io.hypersistence.utils.hibernate.type.range.PostgreSQLRangeType
import io.hypersistence.utils.hibernate.type.range.Range
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDAO
import no.nav.ung.deltakelseopplyser.historikk.BaseAuditEntity
import org.hibernate.annotations.Type
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import org.hibernate.envers.RelationTargetAuditMode
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

@Audited
@Entity(name = "ungdomsprogram_deltakelse")
class DeltakelseDAO(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @NotAudited
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY) // Referer til Deltaker
    @JoinColumn(name = "deltaker_id", referencedColumnName = "id", nullable = false)
    val deltaker: DeltakerDAO,

    @Type(value = PostgreSQLRangeType::class)
    @Column(name = "periode", columnDefinition = "daterange")
    private var periode: Range<LocalDate>,

    @Column(name = "er_slettet")
    var erSlettet: Boolean = false,

    @NotAudited
    @Column(name = "har_opphoersvedtak")
    var harOpphørsvedtak: Boolean = false,

    @Column(name = "søkt_tidspunkt")
    var søktTidspunkt: ZonedDateTime? = null,
) : BaseAuditEntity() {

    fun getFom(): LocalDate {
        return if (periode.hasMask(Range.LOWER_EXCLUSIVE)) periode.lower().plusDays(1) else periode.lower()
    }

    fun getTom(): LocalDate? {
        if (periode.upper() == null) {
            return null
        }
        return if (periode.hasMask(Range.UPPER_EXCLUSIVE)) periode.upper().minusDays(1) else periode.upper()
    }

    /**
     * Oppdaterer periode og endretTidspunkt for denne deltakelsen.
     */
    fun oppdaterPeriode(nyPeriode: Range<LocalDate>) {
        periode = nyPeriode
    }

    fun markerSomHarSøkt() {
        søktTidspunkt = ZonedDateTime.now(ZoneOffset.UTC)
    }

    fun markerSomSlettet() {
        erSlettet = true
    }

    fun markerMedOpphørsvedtak() {
        harOpphørsvedtak = true
    }
}

