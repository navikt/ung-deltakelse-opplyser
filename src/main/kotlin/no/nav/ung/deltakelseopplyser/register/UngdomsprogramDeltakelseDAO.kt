package no.nav.ung.deltakelseopplyser.register

import io.hypersistence.utils.hibernate.type.range.PostgreSQLRangeType
import io.hypersistence.utils.hibernate.type.range.Range
import org.hibernate.annotations.Type
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.ZoneOffset
import java.util.UUID
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate

@Entity(name = "ungdomsprogram_deltakelse")
data class UngdomsprogramDeltakelseDAO(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY) // Referer til Deltaker
    @JoinColumn(name = "deltaker_id", referencedColumnName = "id", nullable = false)
    val deltaker: DeltakerDAO,

    @Type(value = PostgreSQLRangeType::class)
    @Column(name = "periode", columnDefinition = "daterange")
    private val periode: Range<LocalDate>,

    @Column(name = "har_sokt")
    val harSøkt: Boolean,

    @CreatedDate
    @Column(name = "opprettet_tidspunkt")
    val opprettetTidspunkt: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),

    @Column(name = "endret_tidspunkt")
    val endretTidspunkt: ZonedDateTime? = null
)  {

    fun getFom() : LocalDate {
        return if (periode.hasMask(Range.LOWER_EXCLUSIVE)) periode.lower().plusDays(1) else periode.lower()
    }

    fun getTom() : LocalDate? {
        if (periode.upper() == null) {
            return null
        }
        return if (periode.hasMask(Range.UPPER_EXCLUSIVE)) periode.upper().minusDays(1) else periode.upper()
    }
}

