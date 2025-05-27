package no.nav.ung.deltakelseopplyser.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import javax.sql.DataSource

@Configuration
class TxConfiguration {
    companion object {
        const val TRANSACTION_MANAGER = "dstm"
    }

    @Bean(TRANSACTION_MANAGER)
    @Primary
    fun dstm(dataSource: DataSource): DataSourceTransactionManager {
        return DataSourceTransactionManager(dataSource)
    }
}
