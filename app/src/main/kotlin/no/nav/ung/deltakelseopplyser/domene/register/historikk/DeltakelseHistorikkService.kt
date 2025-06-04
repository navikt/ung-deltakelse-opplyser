package no.nav.ung.deltakelseopplyser.domene.register.historikk

import jakarta.persistence.EntityManager
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramDeltakelseDAO
import no.nav.ung.deltakelseopplyser.kontrakt.register.DeltakelseHistorikkDTO
import no.nav.ung.deltakelseopplyser.kontrakt.register.historikk.Endringstype
import no.nav.ung.deltakelseopplyser.kontrakt.register.historikk.Revisjonstype
import org.hibernate.envers.AuditReaderFactory
import org.slf4j.LoggerFactory
import org.springframework.data.history.Revision
import org.springframework.data.history.RevisionMetadata
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneOffset
import java.util.*

@Service
class DeltakelseHistorikkService(
    private val deltakelseHistorikkRepository: DeltakelseHistorikkRepository,
    private val entityManager: EntityManager,
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(DeltakelseHistorikkService::class.java)
    }

    @Transactional(readOnly = true)
    fun deltakelseHistorikk(id: UUID): List<DeltakelseHistorikkDTO> {
        logger.info("Henter historikk for deltakelse med id $id")
        return deltakelseHistorikkRepository.findRevisions(id).stream()
            .map { revision: Revision<Long, UngdomsprogramDeltakelseDAO> ->
                val metadata = revision.metadata
                val endringstype = finnEndretType(
                    revision.entity.id,
                    metadata.revisionNumber.get()
                )
                val deltakelseDAO = revision.entity

                DeltakelseHistorikkDTO(
                    revisjonstype = metadata.revisionType.somHistorikkType(),
                    endringstype = endringstype,
                    revisjonsnummer = metadata.revisionNumber.get(),
                    id = deltakelseDAO.id,
                    fom = deltakelseDAO.getFom(),
                    tom = deltakelseDAO.getTom(),
                    opprettetAv = deltakelseDAO.opprettetAv,
                    opprettetTidspunkt = deltakelseDAO.opprettetTidspunkt.atZone(ZoneOffset.UTC),
                    endretAv = deltakelseDAO.endretAv!!,
                    endretTidspunkt = deltakelseDAO.endretTidspunkt!!.atZone(ZoneOffset.UTC),
                    søktTidspunkt = deltakelseDAO.søktTidspunkt,
                )
            }
            .toList()
            .also {
                logger.info("Fant ${it.size} historikkoppføringer for deltakelse med id $id")
            }
    }

    @Transactional(readOnly = true)
    fun finnEndretType(
        deltakelseId: UUID,
        revisjonsNummer: Number,
    ): Endringstype {
        val auditReader = AuditReaderFactory.get(entityManager)

        // 1) Hente alle revisjonsnummere for entiteten
        val alleRevisjonsnummere: List<Number> =
            auditReader.getRevisions(UngdomsprogramDeltakelseDAO::class.java, deltakelseId)

        // 2) Konverter til Long, fjern duplikater, sorter
        val alleRevLong: List<Long> = alleRevisjonsnummere
            .map { it.toLong() }
            .distinct()
            .sorted()

        // 3) Finn største revisjon < revisjonsNummer
        val prevRev: Long? = alleRevLong
            .filter { it < revisjonsNummer.toLong() }
            .maxOrNull()

        // Dersom vi ikke har en tidligere revisjon, betyr det at dette er den første revisjonen for deltakelsen.
        // Vi tolker dette som at deltakelsen er opprettet og at deltakeren er meldt inn i programmet.
        if (prevRev == null) {
            return Endringstype.DELTAKER_MELDT_INN
        }

        // 5) Henter deltakelsene for "gammel" (prevRev) og "ny" (revisjonsNummer)
        val forrigeDeltakelseRevisjon: UngdomsprogramDeltakelseDAO =
            auditReader.find(UngdomsprogramDeltakelseDAO::class.java, deltakelseId, prevRev)
        val nåværendeDeltakelseRevisjon: UngdomsprogramDeltakelseDAO =
            auditReader.find(UngdomsprogramDeltakelseDAO::class.java, deltakelseId, revisjonsNummer)

        // 6) Sammenligner feltene for å finne ut hva som har endret seg
        val startdatoErEndret = forrigeDeltakelseRevisjon.getFom() != nåværendeDeltakelseRevisjon.getFom()
        val sluttdatoErEndret = forrigeDeltakelseRevisjon.getTom() != nåværendeDeltakelseRevisjon.getTom()
        val soktTidspunktErEndret = forrigeDeltakelseRevisjon.søktTidspunkt != nåværendeDeltakelseRevisjon.søktTidspunkt

        // Lag liste med navn på de feltene som faktisk endret seg
        val endredeFelter = listOfNotNull(
            "startdato".takeIf { startdatoErEndret },
            "sluttdato".takeIf { sluttdatoErEndret },
            "søktTidspunkt".takeIf { soktTidspunktErEndret }
        )

        håndterFlereEndringerISammeRevisjon(endredeFelter, deltakelseId)

            return when {
                startdatoErEndret -> Endringstype.ENDRET_STARTDATO
                sluttdatoErEndret -> Endringstype.ENDRET_SLUTTDATO
                soktTidspunktErEndret -> Endringstype.DELTAKER_HAR_SØKT_YTELSE
                else -> Endringstype.UKJENT
            }
        }

    private fun håndterFlereEndringerISammeRevisjon(endredeFelter: List<String>, deltakelseId: UUID) {
        if (endredeFelter.size >= 2) {
            // Bytt ut siste komma med " og " om det er to eller flere elementer
            val felterTekst = endredeFelter.joinToString(separator = " og ")
            val feilmelding =
                "Deltakelse med id $deltakelseId har endret $felterTekst i samme revisjon. Dette er uvanlig."

            logger.error(feilmelding)
            throw UnsupportedOperationException(feilmelding)
        }
    }


    private fun RevisionMetadata.RevisionType.somHistorikkType() = when (this) {
            RevisionMetadata.RevisionType.INSERT -> Revisjonstype.OPPRETTET
            RevisionMetadata.RevisionType.UPDATE -> Revisjonstype.ENDRET
            RevisionMetadata.RevisionType.DELETE -> Revisjonstype.SLETTET
            RevisionMetadata.RevisionType.UNKNOWN -> Revisjonstype.UKJENT
        }
    }
