package no.nav.ung.deltakelseopplyser.statistikk.oppgave

import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.OppgaveStatus
import no.nav.ung.deltakelseopplyser.statistikk.bigquery.BigQueryRecord
import no.nav.ung.deltakelseopplyser.statistikk.bigquery.BigQueryTabell
import no.nav.ung.deltakelseopplyser.utils.DateUtils
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class OppgaveRapporterInntektRecord(
    val opprettetTidspunkt: ZonedDateTime,
    val oppgaveReferanse: UUID,
    val oppgaveStatus: OppgaveStatus,
    val fom: LocalDate,
    val tom: LocalDate,
    val gjelderDelerAvPerioden: Boolean,
    ) : BigQueryRecord


val RapporterInntektOppgaveTabell: BigQueryTabell<OppgaveRapporterInntektRecord> =
    BigQueryTabell(
        "oppgave_rapporter_inntekt_v2",
        Schema.of(
            Field.of("opprettetTidspunkt", StandardSQLTypeName.DATETIME),
            Field.of("oppgaveReferanse", StandardSQLTypeName.STRING),
            Field.of("oppgaveStatus", StandardSQLTypeName.STRING),
            Field.of("fom", StandardSQLTypeName.DATE),
            Field.of("tom", StandardSQLTypeName.DATE),
            Field.of("gjelderDelerAvPerioden", StandardSQLTypeName.BOOL)
        ),
    ) { rec ->
        mapOf(
            "opprettetTidspunkt" to rec.opprettetTidspunkt.format(DateUtils.DATE_TIME_FORMATTER),
            "oppgaveReferanse" to rec.oppgaveReferanse.toString(),
            "oppgaveStatus" to rec.oppgaveStatus.name,
            "fom" to rec.fom.format(DateTimeFormatter.ISO_LOCAL_DATE),
            "tom" to rec.tom.format(DateTimeFormatter.ISO_LOCAL_DATE),
            "gjelderDelerAvPerioden" to rec.gjelderDelerAvPerioden
        )
    }
