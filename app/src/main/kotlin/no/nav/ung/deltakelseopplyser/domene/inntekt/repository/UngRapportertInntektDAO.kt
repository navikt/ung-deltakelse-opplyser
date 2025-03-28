package no.nav.ung.deltakelseopplyser.domene.inntekt.repository

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.annotation.CreatedDate
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime

@Entity(name = "ung_rapportert_inntekt")
data class UngRapportertInntektDAO(
    @Column(name = "journalpost_id") @Id val journalpostId: String,
    @Column(name = "søker_ident") val søkerIdent: String,
    @Column(name = "inntekt", columnDefinition = "jsonb") @JdbcTypeCode(SqlTypes.JSON) val inntekt: String,
    @Column(name = "opprettet_dato") @CreatedDate val opprettetDato: ZonedDateTime = ZonedDateTime.now(UTC),
    @Column(name = "oppdatert_dato") val oppdatertDato: ZonedDateTime = ZonedDateTime.now(UTC),
) {
    override fun toString(): String {
        return "UngRapportertInntektDAO(journalpostId=$journalpostId, opprettetDato=$opprettetDato, oppdatertDato=$oppdatertDato)"
    }
}
