package no.nav.ung.deltakelseopplyser.statistikk.deltaker

import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import no.nav.ung.deltakelseopplyser.statistikk.bigquery.BigQueryRecord
import no.nav.ung.deltakelseopplyser.statistikk.bigquery.BigQueryTabell
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class AntallDeltakereRecord(
    val antallDeltakere: Long,
    val opprettetTidspunkt: ZonedDateTime,
) : BigQueryRecord


val AntallDeltakereTabell: BigQueryTabell<AntallDeltakereRecord> =
    BigQueryTabell(
        "antall_deltakere",
        Schema.of(
            Field.of("antall_deltakere", StandardSQLTypeName.BIGNUMERIC),
            Field.of("opprettetTidspunkt", StandardSQLTypeName.DATETIME)
        ),
    ) { rec ->
        mapOf(
            "antall_deltakere" to rec.antallDeltakere,
            "opprettetTidspunkt" to rec.opprettetTidspunkt
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"))
        )
    }
