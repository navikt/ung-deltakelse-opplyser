package no.nav.ung.deltakelseopplyser.domene.minside.mikrofrontend

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import no.nav.ung.deltakelseopplyser.domene.deltaker.DeltakerDAO
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.type.SqlTypes
import org.springframework.data.annotation.CreatedDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(name = "min_side_microfrontend_status")
class MinSideMicrofrontendStatusDAO(
    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deltaker_id", referencedColumnName = "id", nullable = false)
    val deltaker: DeltakerDAO,

    @Column(
        name = "status",
        nullable = false,
        length = 10
    ) @Enumerated(EnumType.STRING)
    val status: MicrofrontendStatus,

    @Column(name = "opprettet")
    @CreatedDate
    val opprettet: ZonedDateTime? = null,

    @Column(name = "endret")
    @UpdateTimestamp
    val endret: LocalDateTime? = null
)
