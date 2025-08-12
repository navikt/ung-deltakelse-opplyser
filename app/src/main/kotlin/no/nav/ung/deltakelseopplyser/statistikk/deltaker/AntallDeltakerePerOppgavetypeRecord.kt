package no.nav.ung.deltakelseopplyser.statistikk.deltaker

import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import no.nav.ung.deltakelseopplyser.statistikk.bigquery.BigQueryRecord
import no.nav.ung.deltakelseopplyser.statistikk.bigquery.BigQueryTabell
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class AntallDeltakerePerOppgavetypeRecord(
    val oppgavetype: String,
    val antallDeltakere: Long,
    val opprettetTidspunkt: ZonedDateTime,
): BigQueryRecord

val AntallDeltakerePerOppgavetypeTabell: BigQueryTabell<AntallDeltakerePerOppgavetypeRecord> =
    BigQueryTabell(
        "antall_deltakere_per_oppgavetype_fordeling",
        Schema.of(
            Field.of("antall_deltakere", StandardSQLTypeName.BIGNUMERIC),
            Field.of("oppgavetype", StandardSQLTypeName.STRING),
            Field.of("opprettetTidspunkt", StandardSQLTypeName.DATETIME)
        ),
    ) { rec ->
        mapOf(
            "antall_deltakere" to rec.antallDeltakere,
            "oppgavetype" to rec.oppgavetype,
            "opprettetTidspunkt" to rec.opprettetTidspunkt
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"))
        )
    }
