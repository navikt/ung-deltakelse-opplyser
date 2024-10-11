package no.nav.ung.deltakelseopplyser.register

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import org.springframework.data.annotation.CreatedDate
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

@Entity(name = "ungdomsprogram_deltakelse")
data class UngdomsprogramDeltakelseDAO(
    @Column(name = "id") @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "deltaker_ident") val deltakerIdent: String,
    @Column(name = "fra_og_med") val fraOgMed: LocalDate,
    @Column(name = "til_og_med") val tilOgMed: LocalDate? = null,
    @Column(name = "opprettet_dato") @CreatedDate val opprettetDato: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),
    @Column(name = "oppdatert_dato") val oppdatertDato: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)
)
