package no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RegisterInntektOppgaveDTOTest {


    internal val mapper = ObjectMapper().registerModule(JavaTimeModule())
        .setVisibility(com.fasterxml.jackson.annotation.PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
        .setVisibility(com.fasterxml.jackson.annotation.PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE)
        .setVisibility(com.fasterxml.jackson.annotation.PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .setVisibility(com.fasterxml.jackson.annotation.PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY)


    @Test
    fun `skal serialisere input uten ytelse`() {
        val utenYtelse = """{
  "aktørId": "9915498730261",
  "referanse": "0fc3f6fc-0eb1-4c51-9822-61ab3e71090b",
  "frist": "2025-04-04T07:37:20.251902",
  "fomDato": "2025-01-01",
  "tomDato": "2025-02-01",
  "registerInntekter": {
    "registerinntekterForArbeidOgFrilans": [
      {
        "beløp": 10000,
        "arbeidsgiverIdent": "910909088"
      }
    ]
  }
}"""
        val dto: RegisterInntektOppgaveDTO = mapper.readValue(utenYtelse, RegisterInntektOppgaveDTO::class.java)

        assertEquals("9915498730261", dto.aktørId)
        assertEquals(null, dto.registerInntekter.registerinntekterForYtelse)
        assertEquals(1, dto.registerInntekter.registerinntekterForArbeidOgFrilans?.size)
        assertEquals(10000, dto.registerInntekter.registerinntekterForArbeidOgFrilans?.get(0)?.beløp)
        assertEquals("910909088", dto.registerInntekter.registerinntekterForArbeidOgFrilans?.get(0)?.arbeidsgiverIdent)
    }


    @Test
    fun `skal serialisere input uten arbeidsinntekt`() {
        val utenYtelse = """{
  "aktørId": "9915498730261",
  "referanse": "0fc3f6fc-0eb1-4c51-9822-61ab3e71090b",
  "frist": "2025-04-04T07:37:20.251902",
  "fomDato": "2025-01-01",
  "tomDato": "2025-02-01",
  "registerInntekter": {
  }
}"""
        val dto: RegisterInntektOppgaveDTO = mapper.readValue(utenYtelse, RegisterInntektOppgaveDTO::class.java)

        assertEquals("9915498730261", dto.aktørId)
        assertEquals(null, dto.registerInntekter.registerinntekterForYtelse)
        assertEquals(null, dto.registerInntekter.registerinntekterForArbeidOgFrilans)
    }
}