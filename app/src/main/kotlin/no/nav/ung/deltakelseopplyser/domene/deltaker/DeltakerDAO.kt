package no.nav.ung.deltakelseopplyser.domene.deltaker

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.OppgaveDAO
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramDeltakelseDAO
import java.util.*

@Entity(name = "deltaker")
class DeltakerDAO(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "deltaker_ident", unique = true, nullable = false)
    val deltakerIdent: String,

    @OneToMany(mappedBy = "deltaker") // Refererer til UngdomsprogramDeltakelseDAO
    val deltakelseList: List<UngdomsprogramDeltakelseDAO> = emptyList(),

    // Oppgavene eies direkte av DeltakerDAO med cascade og orphanRemoval for helhetlig håndtering.
    @OneToMany(mappedBy = "deltaker", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
    val oppgaver: MutableSet<OppgaveDAO> = mutableSetOf()
) {

    /**
     * Legger til en ny oppgave i samlingen.
     */
    fun leggTilOppgave(oppgave: OppgaveDAO) {
        oppgaver.add(oppgave)
    }
}
