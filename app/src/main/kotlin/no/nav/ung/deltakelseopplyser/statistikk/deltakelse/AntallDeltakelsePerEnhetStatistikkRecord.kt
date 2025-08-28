package no.nav.ung.deltakelseopplyser.statistikk.deltakelse

import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import no.nav.ung.deltakelseopplyser.statistikk.bigquery.BigQueryRecord
import no.nav.ung.deltakelseopplyser.statistikk.bigquery.BigQueryTabell
import no.nav.ung.deltakelseopplyser.utils.DateUtils
import java.time.ZonedDateTime

data class AntallDeltakelsePerEnhetStatistikkRecord(
    val kontor: String,
    val antallDeltakelser: Int,
    val opprettetTidspunkt: ZonedDateTime,

    val diagnostikk: Map<Any, Any?>, // Brukes for intern diagnostikk, skal ikke lagres i BigQuery.
) : BigQueryRecord

val AntallDeltakelserPerEnhetTabell: BigQueryTabell<AntallDeltakelsePerEnhetStatistikkRecord> =
    BigQueryTabell(
        "antall_deltakere_per_enhet",
        Schema.of(
            Field.of("enhet", StandardSQLTypeName.STRING),
            Field.of("antall_deltakelser", StandardSQLTypeName.BIGNUMERIC),
            Field.of("opprettetTidspunkt", StandardSQLTypeName.DATETIME)
        ),
    ) { rec ->
        mapOf(
            "enhet" to rec.kontor,
            "antall_deltakelser" to rec.antallDeltakelser,
            "opprettetTidspunkt" to rec.opprettetTidspunkt.format(DateUtils.DATE_TIME_FORMATTER)
        )
    }
