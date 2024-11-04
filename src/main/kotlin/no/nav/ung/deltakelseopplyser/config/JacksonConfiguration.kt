package no.nav.ung.deltakelseopplyser.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.ung.deltakelseopplyser.config.JacksonConfiguration.Companion.zonedDateTimeFormatter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Configuration
class JacksonConfiguration {

    companion object {
        val zonedDateTimeFormatter = DateTimeFormatter.ISO_INSTANT.withZone(UTC)
    }

    @Bean
    fun javaTimeModule(): JavaTimeModule {
        return JavaTimeModule().also {
            it.addSerializer(ZonedDateTime::class.java, CustomZonedDateTimeSerializer())
            it.addDeserializer(ZonedDateTime::class.java, CustomZonedDateTimeDeSerializer())
        }
    }
}

class CustomZonedDateTimeSerializer : JsonSerializer<ZonedDateTime?>() {
    override fun serialize(zdt: ZonedDateTime?, gen: JsonGenerator?, serializers: SerializerProvider?) {
        val formattedDate = zdt?.format(zonedDateTimeFormatter)
        gen?.writeString(formattedDate)
    }
}

class CustomZonedDateTimeDeSerializer : JsonDeserializer<ZonedDateTime?>() {

    override fun deserialize(p0: JsonParser, p1: DeserializationContext?): ZonedDateTime {
        return ZonedDateTime.parse(p0?.valueAsString, zonedDateTimeFormatter)
    }
}
