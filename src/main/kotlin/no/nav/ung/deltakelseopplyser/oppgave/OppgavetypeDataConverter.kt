package no.nav.ung.deltakelseopplyser.oppgave

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.postgresql.util.PGobject
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
class OppgavetypeDataConverter : AttributeConverter<OppgavetypeData?, PGobject?> {
    private val objectMapper: ObjectMapper = jacksonObjectMapper().findAndRegisterModules()

    override fun convertToDatabaseColumn(attribute: OppgavetypeData?): PGobject? {
        if (attribute == null) return null
        return try {
            val pgObject = PGobject()
            pgObject.type = "jsonb"
            pgObject.value = objectMapper.writeValueAsString(attribute)
            pgObject
        } catch (ex: Exception) {
            throw IllegalStateException("Kunne ikke konvertere OppgavetypeData til JSON", ex)
        }
    }

    override fun convertToEntityAttribute(dbData: PGobject?): OppgavetypeData? {
        if (dbData == null || dbData.value == null) return null
        return try {
            objectMapper.readValue<OppgavetypeData>(dbData.value!!)
        } catch (ex: Exception) {
            throw IllegalStateException("Kunne ikke konvertere JSON til OppgavetypeData", ex)
        }
    }
}
