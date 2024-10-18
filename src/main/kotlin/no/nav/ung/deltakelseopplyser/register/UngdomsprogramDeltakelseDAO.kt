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

    @Column(name = "deltaker_ident")
    val deltakerIdent: String,

    @Type(value = PostgreSQLRangeType::class)
    @Column(name = "periode", columnDefinition = "daterange")
    val periode: Range<LocalDate>,

    @CreatedDate
    @Column(name = "opprettet_tidspunkt")
    val opprettetTidspunkt: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),

    @Column(name = "endret_tidspunkt")
    val endretTidspunkt: ZonedDateTime? = null
)

