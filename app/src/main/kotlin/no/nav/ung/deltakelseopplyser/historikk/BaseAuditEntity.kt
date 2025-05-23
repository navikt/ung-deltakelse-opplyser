package no.nav.ung.deltakelseopplyser.historikk

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.hibernate.envers.Audited
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Audited
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseAuditEntity {

    @CreatedBy
    @Column(name = "opprettet_av", updatable = false)
    var opprettetAv: String? = null

    @CreatedDate
    @Column(name = "opprettet_tidspunkt", updatable = false)
    var opprettetTidspunkt: Instant = Instant.now()

    @LastModifiedDate
    @Column(name = "endret_tidspunkt")
    var endretTidspunkt: Instant? = null

    @LastModifiedBy
    @Column(name = "endret_av")
    var endretAv: String? = null
}
