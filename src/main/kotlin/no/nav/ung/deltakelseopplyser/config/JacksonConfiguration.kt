package no.nav.ung.deltakelseopplyser.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

@Configuration
class JacksonConfiguration {

    @Bean
    fun jackson2ObjectMapperBuilderCustomizer(builder: Jackson2ObjectMapperBuilder) = builder
        .modules(JavaTimeModule())
        .build<ObjectMapper>()
}
