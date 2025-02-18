package no.nav.ung.deltakelseopplyser.soknad.repository

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.annotation.CreatedDate
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime

@Entity(name = "ung_søknad")
data class UngSøknadDAO(
    @Column(name = "journalpost_id") @Id val journalpostId: String,
    @Column(name = "søker_ident") val søkerIdent: String,
    @Column(name = "søknad", columnDefinition = "jsonb") @JdbcTypeCode(SqlTypes.JSON) val søknad: String,
    @Column(name = "opprettet_dato") @CreatedDate val opprettetDato: ZonedDateTime = ZonedDateTime.now(UTC),
    @Column(name = "oppdatert_dato") val oppdatertDato: ZonedDateTime = ZonedDateTime.now(UTC),
) {
    override fun toString(): String {
        return "SøknadDAO(journalpostId=$journalpostId, opprettetDato=$opprettetDato, oppdatertDato=$oppdatertDato)"
    }
}
