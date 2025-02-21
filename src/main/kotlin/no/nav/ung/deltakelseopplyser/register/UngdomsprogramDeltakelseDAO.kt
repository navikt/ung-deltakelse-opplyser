package no.nav.ung.deltakelseopplyser.register

import io.hypersistence.utils.hibernate.type.range.PostgreSQLRangeType
import io.hypersistence.utils.hibernate.type.range.Range
import org.hibernate.annotations.Type
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.ZoneOffset
import java.util.UUID
import jakarta.persistence.*
import no.nav.ung.deltakelseopplyser.oppgave.OppgaveDAO
import org.springframework.data.annotation.CreatedDate

@Entity(name = "ungdomsprogram_deltakelse")
class UngdomsprogramDeltakelseDAO(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY) // Referer til Deltaker
    @JoinColumn(name = "deltaker_id", referencedColumnName = "id", nullable = false)
    val deltaker: DeltakerDAO,

    @Type(value = PostgreSQLRangeType::class)
    @Column(name = "periode", columnDefinition = "daterange")
    private var periode: Range<LocalDate>,

    @Column(name = "har_sokt")
    var harSøkt: Boolean,

    /**
     * Oppgaver som er knyttet til deltakelsen.
     * Oppgavene knyttes til denne deltakelsen via feltet `deltakelse` i OppgaveDAO.
     */
    @OneToMany(mappedBy = "deltakelse", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val oppgaver: MutableSet<OppgaveDAO> = mutableSetOf(),

    @CreatedDate
    @Column(name = "opprettet_tidspunkt")
    val opprettetTidspunkt: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),

    @Column(name = "endret_tidspunkt")
    var endretTidspunkt: ZonedDateTime? = null,
) {

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
     * Legger til en ny oppgave i samlingen.
     */
    fun leggTilOppgave(oppgave: OppgaveDAO) {
        oppgaver.add(oppgave)
    }

    /**
     * Oppdaterer en eksisterende oppgave. Oppgaven med samme id blir erstattet.
     */
    fun oppdaterOppgave(oppdatertOppgave: OppgaveDAO) {
        oppgaver.removeAll { it.id == oppdatertOppgave.id }
        oppgaver.add(oppdatertOppgave)
    }

    /**
     * Oppdaterer periode og endretTidspunkt for denne deltakelsen.
     */
    fun oppdaterPeriode(nyPeriode: Range<LocalDate>) {
        periode = nyPeriode
        endretTidspunkt = ZonedDateTime.now(ZoneOffset.UTC)
    }

    fun markerSomHarSøkt() {
        harSøkt = true
        endretTidspunkt = ZonedDateTime.now(ZoneOffset.UTC)
    }
}

