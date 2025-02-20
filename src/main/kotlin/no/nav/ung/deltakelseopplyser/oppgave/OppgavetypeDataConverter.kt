package no.nav.ung.deltakelseopplyser.oppgave

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.time.LocalDate

@OppgavetypeDataJsonType
sealed class OppgavetypeData

data class EndretStartdatoOppgavetypeData(
    val nyStartdato: LocalDate,
) : OppgavetypeData()

data class EndretSluttdatoOppgavetypeData(
    val nySluttdato: LocalDate,
) : OppgavetypeData()

@Converter(autoApply = true)
class OppgavetypeDataConverter : AttributeConverter<OppgavetypeData?, String?> {
    private val objectMapper: ObjectMapper = jacksonObjectMapper().findAndRegisterModules()

    override fun convertToDatabaseColumn(attribute: OppgavetypeData?): String? {
        return attribute?.let { objectMapper.writeValueAsString(it) }
    }

    override fun convertToEntityAttribute(dbData: String?): OppgavetypeData? {
        return dbData?.let { objectMapper.readValue<OppgavetypeData>(it) }
    }
}
