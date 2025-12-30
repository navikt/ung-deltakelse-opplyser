package no.nav.ung.deltakelseopplyser.statistikk.bigquery

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryException
import com.google.cloud.bigquery.DatasetId
import com.google.cloud.bigquery.InsertAllRequest
import com.google.cloud.bigquery.InsertAllResponse
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardTableDefinition
import com.google.cloud.bigquery.TableId
import com.google.cloud.bigquery.TableInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime


interface BigQueryClient {
    fun <T> publish(
        dataset: String,
        tableDef: BigQueryTabell<T>,
        records: Collection<T>
    )

    fun <T> finnSisteOppdateringAvTabell(
        dataset: String,
        tableDef: BigQueryTabell<T>,
    ): ZonedDateTime?
}

@Service
class BigQueryKlient(private val bigQuery: BigQuery): BigQueryClient {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(BigQueryKlient::class.java)
    }



    override fun <T> publish(
        dataset: String,
        tableDef: BigQueryTabell<T>,
        records: Collection<T>
    ) {
        this.forsikreDatasetEksisterer(dataset)
        val tableId = finnTabell(tableDef, dataset)
        val rader = records.stream().map { tableDef.tilRowInsert(it) }.toList()
        val request = InsertAllRequest.newBuilder(tableId)
            .setRows(rader)
            .build()
        val insertAllResponse = bigQuery.insertAll(request)
        håndterResponse(insertAllResponse, request, tableDef, dataset)
    }

    override fun <T> finnSisteOppdateringAvTabell(
        dataset: String,
        tableDef: BigQueryTabell<T>
    ): ZonedDateTime? {
        this.forsikreDatasetEksisterer(dataset)
        val table = bigQuery.getTable(TableId.of(dataset, tableDef.tabellNavn))
        if (table == null) {
            logger.warn("Fant ikke tabell ${tableDef.tabellNavn} i datasettet $dataset, kan derfor ikke finne siste oppdateringstid.")
            return null
        }
        logger.info("Fant tabell ${tableDef.tabellNavn} i datasettet $dataset med siste oppdateringstid ${table.lastModifiedTime}")
        return table.lastModifiedTime?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.of("Europe/Oslo"))
        }
    }

    private fun <T> finnTabell(tableDef: BigQueryTabell<T>, dataset: String): TableId {
        val table = bigQuery.getTable(TableId.of(dataset, tableDef.tabellNavn))

        if (table != null) {
            logger.info("Bruker eksisterende tabell: ${tableDef.tabellNavn} i dataset: $dataset")
            return table.tableId
        }

        val tableId = TableId.of(dataset, tableDef.tabellNavn)
        val tableDefinition = StandardTableDefinition.newBuilder().setSchema(tableDef.skjema).build()

        val tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build()

        bigQuery.create(tableInfo)
        logger.info("Opprettet ny tabell: ${tableId}")
        return tableId
    }


    /**
     * Forsikrer at et BigQuery-datasett eksisterer.
     *
     * @param bigQueryDataset Navnet på BigQuery-datasettet som skal sjekkes.
     * @throws RuntimeException hvis datasettet ikke eksisterer.
     */
    private fun forsikreDatasetEksisterer(bigQueryDataset: String) {
        try {
            val dataset = bigQuery.getDataset(DatasetId.of(bigQueryDataset))
            if (dataset != null) {
                logger.info("Forsikret at dataset {} eksisterer i BigQuery.", bigQueryDataset)
            } else {
                logger.error(
                    "Dataset {} eksisterer ikke i BigQuery. Opprett en dataset i BigQuery før du publiserer data.",
                    bigQueryDataset
                )
                throw java.lang.RuntimeException("Dataset $bigQueryDataset eksisterer ikke i BigQuery. Opprett dataset før publisering.")
            }
        } catch (e: BigQueryException) {
            logger.error("Noe gikk galt ved forsøk på å hente dataset {}: {}", bigQueryDataset, e.message, e)
            throw java.lang.RuntimeException("Kunne ikke hente dataset $bigQueryDataset fra BigQuery.", e)
        }
    }

    private fun <T> håndterResponse(
        insertAllResponse: InsertAllResponse,
        request: InsertAllRequest,
        tableDef: BigQueryTabell<T>,
        dataset: String
    ) {
        if (insertAllResponse.hasErrors()) {
            insertAllResponse.insertErrors.forEach(
                { (index, errors) ->
                    logger.error("Feil ved innsending av rad $index: ${errors.joinToString(", ")}")
                }
            )
            throw RuntimeException("Feil ved innsending av data til BigQuery: ${insertAllResponse.insertErrors}")
        } else {
            logger.info("${request.rows.size} rader sendt til BigQuery tabell: ${tableDef.tabellNavn} i dataset: ${dataset}")
        }
    }

}

interface BigQueryRecord {
}

class BigQueryTabell<BigQueryRecord>(
    val tabellNavn: String,
    val skjema: Schema,
    val mapperFunksjon: (BigQueryRecord) -> Map<String, Any?>,
) {

    fun tilRowInsert(record: BigQueryRecord): InsertAllRequest.RowToInsert {
        return InsertAllRequest.RowToInsert.of(mapperFunksjon(record))
    }
}
