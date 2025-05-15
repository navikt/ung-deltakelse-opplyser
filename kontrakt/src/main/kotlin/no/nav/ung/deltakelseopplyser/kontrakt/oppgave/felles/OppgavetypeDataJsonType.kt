package no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Denne annotasjonen brukes for polymorf serialisering og deserialisering
 * av oppgavetype-data. Ved å bruke denne annotasjonen vil Jackson inkludere
 * en "type"-egenskap i JSON-representasjonen som angir hvilken konkret subtype
 * som skal benyttes (f.eks. "BEKREFT_ENDRET_PROGRAMPERIODE" eller "BEKREFT_AVVIK_REGISTERINNTEKT").
 *
 * Denne annotasjonen kan brukes på både entiteter og DTO-er for å unngå duplisering
 * av Jacksons @JsonTypeInfo og @JsonSubTypes-annotasjoner.
 */
@JacksonAnnotationsInside
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    // Endret programperiode oppgavetype data
    JsonSubTypes.Type(value = EndretProgramperiodeDataDTO::class, name = "BEKREFT_ENDRET_PROGRAMPERIODE"),

    // Kontroller registerinntekt oppgavetype data
    JsonSubTypes.Type(
        value = KontrollerRegisterinntektOppgavetypeDataDTO::class,
        name = "BEKREFT_AVVIK_REGISTERINNTEKT"
    ),

    // Inntektsrapportering oppgavetype data
    JsonSubTypes.Type(
        value = InntektsrapporteringOppgavetypeDataDTO::class,
        name = "INNTEKTSRAPPORTERING"
    )
)
annotation class OppgavetypeDataJsonType
