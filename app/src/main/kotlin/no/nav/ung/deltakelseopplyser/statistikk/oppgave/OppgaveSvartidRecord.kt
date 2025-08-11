package no.nav.ung.deltakelseopplyser.statistikk.oppgave

import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype
import no.nav.ung.deltakelseopplyser.statistikk.bigquery.BigQueryRecord
import no.nav.ung.deltakelseopplyser.statistikk.bigquery.BigQueryTabell
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class OppgaveSvartidRecord(
    val svartidAntallDager: Long?,
    val erLøst: Boolean,
    val erLukket: Boolean,
    val ikkeMottattOgEldreEnn14Dager: Boolean,
    val oppgaveType: Oppgavetype,
    val antall: Int,
    val opprettetTidspunkt: ZonedDateTime,

    ) : BigQueryRecord


val OppgaveSvartidTabell: BigQueryTabell<OppgaveSvartidRecord> =
    BigQueryTabell(
        "oppgave_svartid",
        Schema.of(
            Field.of("antall", StandardSQLTypeName.BIGNUMERIC),
            Field.of("svartidAntallDager", StandardSQLTypeName.NUMERIC),
            Field.of("erLøst", StandardSQLTypeName.BOOL),
            Field.of("erLukket", StandardSQLTypeName.BOOL),
            Field.of("ikkeMottattOgEldreEnn14Dager", StandardSQLTypeName.BOOL),
            Field.of("oppgaveType", StandardSQLTypeName.STRING),
            Field.of("opprettetTidspunkt", StandardSQLTypeName.DATETIME)
        ),
    ) { rec ->
        mapOf(
            "antall" to rec.antall,
            "svartidAntallDager" to rec.svartidAntallDager,
            "opprettetTidspunkt" to rec.opprettetTidspunkt
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")),
            "erLøst" to rec.erLøst,
            "erLukket" to rec.erLukket,
            "ikkeMottattOgEldreEnn14Dager" to rec.ikkeMottattOgEldreEnn14Dager,
            "oppgaveType" to rec.oppgaveType.name
        )
    }
