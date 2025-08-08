package no.nav.ung.deltakelseopplyser.statistikk.bigquery

import com.google.cloud.bigquery.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service


interface BigQueryClient {
    fun <T> publish(
        tableDef: BigQueryTabell<T>,
        records: Collection<T>
    )
}

@Service
class BigQueryOppgaveStatistikkKlient(private val bigQuery: BigQuery): BigQueryClient {

    companion object {
        private val BIG_QUERY_DATASET: String = "ung_sak_statistikk_dataset"
        private val logger: Logger = LoggerFactory.getLogger(BigQueryOppgaveStatistikkKlient::class.java)
    }

    init {
        this.forsikreDatasetEksisterer(BIG_QUERY_DATASET)
    }

    override fun <T> publish(
        tableDef: BigQueryTabell<T>,
        records: Collection<T>
    ) {
        val tableId = finnTabell(tableDef)
        val rader = records.stream().map { tableDef.tilRowInsert(it) }.toList()
        val request = InsertAllRequest.newBuilder(tableId)
            .setRows(rader)
            .build()
        val insertAllResponse = bigQuery.insertAll(request)
        håndterResponse(insertAllResponse, request, tableDef, BIG_QUERY_DATASET)
    }


    private fun <T> finnTabell(tableDef: BigQueryTabell<T>): TableId {
        val table = bigQuery.getTable(TableId.of(BIG_QUERY_DATASET, tableDef.tabellNavn))

        if (table != null) {
            logger.info("Bruker eksisterende tabell: ${tableDef.tabellNavn} i dataset: $BIG_QUERY_DATASET")
            return table.tableId
        }

        val tableId = TableId.of(BIG_QUERY_DATASET, tableDef.tabellNavn)
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
