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

/**
 * Baseentitet for alle entiteter som skal ha historikk.
 * Må huske å annotere den klassen eller felter i den med @Audited.
 *
 * @CreatedBy og @LastModifiedBy for å sette inn hvem som har opprettet og endret en entitet.
 * @CreatedDate og @LastModifiedDate for å sette inn når en entitet ble opprettet og endret.
 */
@Audited
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseAuditEntity {

    @CreatedBy
    @Column(name = "opprettet_av", updatable = false)
    lateinit var opprettetAv: String

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
