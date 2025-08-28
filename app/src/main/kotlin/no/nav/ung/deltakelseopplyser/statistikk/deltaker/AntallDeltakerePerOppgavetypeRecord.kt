package no.nav.ung.deltakelseopplyser.statistikk.deltaker

import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import no.nav.ung.deltakelseopplyser.statistikk.bigquery.BigQueryRecord
import no.nav.ung.deltakelseopplyser.statistikk.bigquery.BigQueryTabell
import no.nav.ung.deltakelseopplyser.utils.DateUtils
import java.time.ZonedDateTime

data class AntallDeltakerePerOppgavetypeRecord(
    val oppgavetype: String,
    val oppgavestatus: String,
    val antallDeltakere: Long,
    val opprettetTidspunkt: ZonedDateTime,
): BigQueryRecord

val AntallDeltakerePerOppgavetypeTabell: BigQueryTabell<AntallDeltakerePerOppgavetypeRecord> =
    BigQueryTabell(
        "antall_deltakere_per_oppgavetype_status_fordeling",
        Schema.of(
            Field.of("antall_deltakere", StandardSQLTypeName.BIGNUMERIC),
            Field.of("oppgavetype", StandardSQLTypeName.STRING),
            Field.of("oppgavestatus", StandardSQLTypeName.STRING),
            Field.of("opprettetTidspunkt", StandardSQLTypeName.DATETIME)
        ),
    ) { rec ->
        mapOf(
            "antall_deltakere" to rec.antallDeltakere,
            "oppgavetype" to rec.oppgavetype,
            "oppgavestatus" to rec.oppgavestatus,
            "opprettetTidspunkt" to rec.opprettetTidspunkt.format(DateUtils.DATE_TIME_FORMATTER)
        )
    }
