package no.nav.ung.deltakelseopplyser.domene.oppgave

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.EndretSluttdatoOppgavetypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.EndretStartdatoOppgavetypeDataDAO
import no.nav.ung.deltakelseopplyser.domene.oppgave.repository.KontrollerRegisterInntektOppgaveTypeDataDAO

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
    JsonSubTypes.Type(value = EndretStartdatoOppgavetypeDataDAO::class, name = "BEKREFT_ENDRET_STARTDATO"),

    // Endret sluttdato oppgavetype data
    JsonSubTypes.Type(value = EndretSluttdatoOppgavetypeDataDTO::class, name = "BEKREFT_ENDRET_SLUTTDATO"),
    JsonSubTypes.Type(value = EndretSluttdatoOppgavetypeDataDAO::class, name = "BEKREFT_ENDRET_SLUTTDATO"),

    // Kontroller registerinntekt oppgavetype data
    JsonSubTypes.Type(value = KontrollerRegisterinntektOppgavetypeDataDTO::class, name = "BEKREFT_AVVIK_REGISTERINNTEKT"),
    JsonSubTypes.Type(value = KontrollerRegisterInntektOppgaveTypeDataDAO::class, name = "BEKREFT_AVVIK_REGISTERINNTEKT")
)
annotation class OppgavetypeDataJsonType
