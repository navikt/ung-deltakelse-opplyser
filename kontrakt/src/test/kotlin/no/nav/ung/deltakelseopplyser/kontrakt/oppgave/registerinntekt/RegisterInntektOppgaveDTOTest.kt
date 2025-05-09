package no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

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
        //language=JSON
        val utenYtelse = """
            {
              "deltakerIdent": "99154987302",
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

        assertEquals("99154987302", dto.deltakerIdent)
        assertEquals(null, dto.registerInntekter.registerinntekterForYtelse)
        assertEquals(1, dto.registerInntekter.registerinntekterForArbeidOgFrilans?.size)
        assertEquals(10000, dto.registerInntekter.registerinntekterForArbeidOgFrilans?.get(0)?.beløp)
        assertEquals("910909088", dto.registerInntekter.registerinntekterForArbeidOgFrilans?.get(0)?.arbeidsgiverIdent)
    }

    @ParameterizedTest
    @EnumSource(YtelseType::class)
    fun `skal serialisere input med ytelse`(ytelseType: YtelseType) {
        //language=JSON
        val utenYtelse = """
            {
              "deltakerIdent": "99154987302",
              "referanse": "0fc3f6fc-0eb1-4c51-9822-61ab3e71090b",
              "frist": "2025-04-04T07:37:20.251902",
              "fomDato": "2025-01-01",
              "tomDato": "2025-02-01",
              "registerInntekter": {
                "registerinntekterForYtelse": [
                  {
                    "beløp": 10000,
                    "ytelseType": "${ytelseType.kode}"
                  }
              ]
            }
            }"""
        val dto: RegisterInntektOppgaveDTO = mapper.readValue(utenYtelse, RegisterInntektOppgaveDTO::class.java)

        assertEquals("99154987302", dto.deltakerIdent)
        assertEquals(1, dto.registerInntekter.registerinntekterForYtelse?.size)

        val inntektYtelseDTO = dto.registerInntekter.registerinntekterForYtelse?.first()
        assertEquals(10000, inntektYtelseDTO?.beløp)
        assertEquals(ytelseType, inntektYtelseDTO?.ytelseType)
    }


    @Test
    fun `skal serialisere input uten arbeidsinntekt`() {
        //language=JSON
        val utenYtelse = """
            {
              "deltakerIdent": "99154987302",
              "referanse": "0fc3f6fc-0eb1-4c51-9822-61ab3e71090b",
              "frist": "2025-04-04T07:37:20.251902",
              "fomDato": "2025-01-01",
              "tomDato": "2025-02-01",
              "registerInntekter": {
              }
            }"""
        val dto: RegisterInntektOppgaveDTO = mapper.readValue(utenYtelse, RegisterInntektOppgaveDTO::class.java)

        assertEquals("99154987302", dto.deltakerIdent)
        assertEquals(null, dto.registerInntekter.registerinntekterForYtelse)
        assertEquals(null, dto.registerInntekter.registerinntekterForArbeidOgFrilans)
    }
}
