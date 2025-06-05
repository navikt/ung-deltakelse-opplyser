package no.nav.ung.deltakelseopplyser.domene.register.historikk

import no.nav.ung.deltakelseopplyser.domene.register.DeltakelseDAO
import no.nav.ung.deltakelseopplyser.kontrakt.register.historikk.Revisjonstype
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
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(DeltakelseHistorikkService::class.java)
    }

    @Transactional(readOnly = true)
    fun deltakelseHistorikk(id: UUID): List<DeltakelseHistorikk> {
        logger.info("Henter historikk for deltakelse med id $id")

        val alleRevisjoner: List<Revision<Long, DeltakelseDAO>> =
            deltakelseHistorikkRepository.findRevisions(id)
                .toList()
                .sortedBy { it.requiredRevisionInstant }

        if (alleRevisjoner.isEmpty()) {
            return emptyList()
        }

        // Mapper over listen med indekser, slik at vi enkelt kan hente "forrige" revisjon
        return alleRevisjoner.mapIndexed { index, revision ->
                val metadata = revision.metadata

                val nåværendeRevisjon = revision.entity

                // Henter forrige revisjon hvis den finnes
                val forrigeRevisjon: DeltakelseDAO? =
                    alleRevisjoner.getOrNull(index - 1)?.entity

                val historikkEndring = DeltakelseHistorikkEndringUtleder.utledEndring(
                    forrigeDeltakelseRevisjon = forrigeRevisjon,
                    nåværendeDeltakelseRevisjon = nåværendeRevisjon
                )

                DeltakelseHistorikk(
                    revisjonstype = metadata.revisionType.somHistorikkType(),
                    endringstype = historikkEndring.endringstype,
                    revisjonsnummer = metadata.revisionNumber.get(),
                    deltakelse = nåværendeRevisjon,
                    opprettetAv = nåværendeRevisjon.opprettetAv!!,
                    opprettetTidspunkt = nåværendeRevisjon.opprettetTidspunkt.atZone(ZoneOffset.UTC),
                    endretAv = nåværendeRevisjon.endretAv,
                    endretTidspunkt = nåværendeRevisjon.endretTidspunkt!!.atZone(ZoneOffset.UTC),
                    endretStartdato = historikkEndring.endretStartdatoDataDTO,
                    endretSluttdato = historikkEndring.endretSluttdatoDataDTO,
                    søktTidspunktSatt = historikkEndring.søktTidspunktSatt
                )
            }.also {
                logger.info("Fant ${it.size} historikkoppføringer for deltakelse med id $id")
            }
    }

    private fun RevisionMetadata.RevisionType.somHistorikkType() = when (this) {
        RevisionMetadata.RevisionType.INSERT -> Revisjonstype.OPPRETTET
        RevisionMetadata.RevisionType.UPDATE -> Revisjonstype.ENDRET
        RevisionMetadata.RevisionType.DELETE -> Revisjonstype.SLETTET
        RevisionMetadata.RevisionType.UNKNOWN -> Revisjonstype.UKJENT
    }
}
