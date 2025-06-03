package no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretSluttdatoOppgaveDTO
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretStartdatoOppgaveDTO

/**
 * Denne annotasjonen brukes for polymorf serialisering og deserialisering
 * av oppgavetype-data. Ved å bruke denne annotasjonen vil Jackson inkludere
 * en "type"-egenskap i JSON-representasjonen som angir hvilken konkret subtype
 * som skal benyttes (f.eks. "BEKREFT_ENDRET_STARTDATO" eller "BEKREFT_AVVIK_REGISTERINNTEKT").
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
    JsonSubTypes.Type(
        value = EndretStartdatoDataDTO::class,
        name = "BEKREFT_ENDRET_STARTDATO"
    ),

    // Endret sluttdato oppgavetype data
    JsonSubTypes.Type(
        value = EndretSluttdatoDataDTO::class,
        name = "BEKREFT_ENDRET_SLUTTDATO"
    ),

    // Kontroller registerinntekt oppgavetype data
    JsonSubTypes.Type(
        value = KontrollerRegisterinntektOppgavetypeDataDTO::class,
        name = "BEKREFT_AVVIK_REGISTERINNTEKT"
    ),

    // Inntektsrapportering oppgavetype data
    JsonSubTypes.Type(
        value = InntektsrapporteringOppgavetypeDataDTO::class,
        name = "RAPPORTER_INNTEKT"
    ),

    // Send søknad oppgavetype data
    JsonSubTypes.Type(
        value = SøkYtelseOppgavetypeDataDTO::class,
        name = "SØK_YTELSE"
    )
)
annotation class OppgavetypeDataJsonType
