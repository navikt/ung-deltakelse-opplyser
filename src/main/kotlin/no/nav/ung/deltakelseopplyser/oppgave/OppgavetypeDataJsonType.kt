package no.nav.ung.deltakelseopplyser.oppgave

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Denne annotasjonen brukes for polymorf serialisering og deserialisering
 * av oppgavetype-data. Ved å bruke denne annotasjonen vil Jackson inkludere
 * en "type"-egenskap i JSON-representasjonen som angir hvilken konkret subtype
 * som skal benyttes (f.eks. "BEKREFT_ENDRET_STARTDATO" eller "BEKREFT_ENDRET_SLUTTDATO").
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
    // Endret startdato oppgavetype data
    JsonSubTypes.Type(value = EndretStartdatoOppgavetypeDataDTO::class, name = "BEKREFT_ENDRET_STARTDATO"),
    JsonSubTypes.Type(value = EndretStartdatoOppgavetypeData::class, name = "BEKREFT_ENDRET_STARTDATO"),

    // Endret sluttdato oppgavetype data
    JsonSubTypes.Type(value = EndretSluttdatoOppgavetypeDataDTO::class, name = "BEKREFT_ENDRET_SLUTTDATO"),
    JsonSubTypes.Type(value = EndretSluttdatoOppgavetypeData::class, name = "BEKREFT_ENDRET_SLUTTDATO"),
)
annotation class OppgavetypeDataJsonType
