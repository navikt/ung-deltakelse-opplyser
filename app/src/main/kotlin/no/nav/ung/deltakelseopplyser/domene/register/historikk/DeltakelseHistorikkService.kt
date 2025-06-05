package no.nav.ung.deltakelseopplyser.domene.register.historikk

import jakarta.persistence.EntityManager
import no.nav.ung.deltakelseopplyser.domene.register.UngdomsprogramDeltakelseDAO
import no.nav.ung.deltakelseopplyser.kontrakt.register.historikk.Revisjonstype
import org.hibernate.envers.AuditReader
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
    fun deltakelseHistorikk(id: UUID): List<DeltakelseHistorikk> {
        logger.info("Henter historikk for deltakelse med id $id")
        return deltakelseHistorikkRepository.findRevisions(id).stream()
            .map { revision: Revision<Long, UngdomsprogramDeltakelseDAO> ->
                val metadata = revision.metadata
                val historikkEndring: DeltakelseHistorikkEndringUtleder.HistorikkEndring = utledEndring(
                    revision.entity.id,
                    metadata.revisionNumber.get()
                )
                val deltakelseDAO = revision.entity

                DeltakelseHistorikk(
                    revisjonstype = metadata.revisionType.somHistorikkType(),
                    endringstype = historikkEndring.endringstype,
                    revisjonsnummer = metadata.revisionNumber.get(),
                    deltakelse = deltakelseDAO,
                    opprettetAv = deltakelseDAO.opprettetAv!!,
                    opprettetTidspunkt = deltakelseDAO.opprettetTidspunkt.atZone(ZoneOffset.UTC),
                    endretAv = deltakelseDAO.endretAv,
                    endretTidspunkt = deltakelseDAO.endretTidspunkt!!.atZone(ZoneOffset.UTC),
                    endretStartdato = historikkEndring.endretStartdatoDataDTO,
                    endretSluttdato = historikkEndring.endretSluttdatoDataDTO,
                    søktTidspunktSatt = historikkEndring.søktTidspunktSatt
                )
            }
            .toList()
            .also {
                logger.info("Fant ${it.size} historikkoppføringer for deltakelse med id $id")
            }
    }

    @Transactional(readOnly = true)
    fun utledEndring(
        deltakelseId: UUID,
        revisjonsnummer: Number,
    ): DeltakelseHistorikkEndringUtleder.HistorikkEndring {
        val auditReader = AuditReaderFactory.get(entityManager)

        // Henter forrige revisjonsnummer for deltakelsen
        val forrigeRevisjonsnummer = hentForrigeRevisjonsnummer(auditReader, deltakelseId, revisjonsnummer)

        // Henter deltakelsene for "gammel" (prevRev) og "ny" (revisjonsNummer)
        val forrigeDeltakelseRevisjon =
            forrigeRevisjonsnummer?.let { auditReader.find(UngdomsprogramDeltakelseDAO::class.java, deltakelseId, it) }

        val nåværendeDeltakelseRevisjon =
            auditReader.find(UngdomsprogramDeltakelseDAO::class.java, deltakelseId, revisjonsnummer)

        return DeltakelseHistorikkEndringUtleder(
            nåværendeDeltakelseRevisjon = nåværendeDeltakelseRevisjon,
            forrigeDeltakelseRevisjon = forrigeDeltakelseRevisjon
        ).utledEndring()
    }


    private fun RevisionMetadata.RevisionType.somHistorikkType() = when (this) {
        RevisionMetadata.RevisionType.INSERT -> Revisjonstype.OPPRETTET
        RevisionMetadata.RevisionType.UPDATE -> Revisjonstype.ENDRET
        RevisionMetadata.RevisionType.DELETE -> Revisjonstype.SLETTET
        RevisionMetadata.RevisionType.UNKNOWN -> Revisjonstype.UKJENT
    }

    private fun hentForrigeRevisjonsnummer(
        auditReader: AuditReader,
        deltakelseId: UUID,
        revisjonsNummer: Number,
    ): Long? {
        return auditReader.getRevisions(UngdomsprogramDeltakelseDAO::class.java, deltakelseId)
            .asSequence()
            .map { it.toLong() }
            .distinct()
            .sorted()
            .filter { it < revisjonsNummer.toLong() }
            .maxOrNull()
    }
}
