# Outbox Pattern Implementation

## Oversikt

Outbox pattern implementeringen sikrer pålitelig publisering av hendelser ved å lagre dem i en database tabell som en del av samme transaksjon som forretningsoperasjonen. En separat prosess leser og publiserer hendelsene asynkront.

Denne modulen er en selvstendig Spring Boot library som kan gjenbrukes på tvers av prosjekter.

## Komponenter

### Database
- **outbox tabell**: Lagrer alle hendelser som skal publiseres
- **V16__outbox_table.sql**: Database migrasjon som oppretter tabellen

### Klasser
- **OutboxDAO**: Entity som representerer en hendelse i outbox tabellen
- **OutboxRepository**: Repository for database operasjoner
- **OutboxProducer**: Service for å publisere hendelser til outbox
- **OutboxConsumer**: Service for å prosessere hendelser fra outbox
- **OutboxScheduler**: Scheduler som kjører konsument prosessen periodisk
- **OutboxEvent**: Interface for hendelser
- **OutboxEventHandler**: Interface for håndtering av spesifikke hendelser
- **OutboxAutoConfiguration**: Spring Boot auto-konfigurering

## Installasjon

### Maven Avhengighet

Legg til følgende avhengighet i ditt prosjekts `pom.xml`:

```xml
<dependency>
    <groupId>no.nav.ung.deltakelseopplyser</groupId>
    <artifactId>outbox</artifactId>
    <version>${project.version}</version>
</dependency>
```

### Database Setup

Sørg for at Flyway er konfigurert til å kjøre migrasjoner fra outbox-modulen. Outbox-tabellen blir automatisk opprettet ved første oppstart.

## Bruk

### 1. Opprett en hendelse

```kotlin
package com.example.events

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.ung.deltakelseopplyser.outbox.OutboxEvent
import java.time.LocalDateTime

data class MinHendelseEvent(
    override val aggregateId: String,
    val data: String,
    val timestamp: String = LocalDateTime.now().toString()
) : OutboxEvent {
    override val eventType: String = "MIN_HENDELSE"
    
    override fun toJson(): String {
        return ObjectMapper().writeValueAsString(this)
    }
}
```

### 2. Opprett en hendelse handler

```kotlin
package com.example.handlers

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.ung.deltakelseopplyser.outbox.OutboxDAO
import no.nav.ung.deltakelseopplyser.outbox.OutboxEventHandler
import com.example.events.MinHendelseEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class MinHendelseHandler(
    private val objectMapper: ObjectMapper
) : OutboxEventHandler {
    
    private val logger = LoggerFactory.getLogger(MinHendelseHandler::class.java)
    
    override val eventType: String = "MIN_HENDELSE"
    
    override fun handle(event: OutboxDAO) {
        val minHendelse = objectMapper.readValue(event.payload, MinHendelseEvent::class.java)
        
        // Implementer behandling av hendelsen:
        // - Send til Kafka
        // - Kall eksterne APIer
        // - Send notifikasjoner
        // - Oppdater cache
        // etc.
        
        logger.info("Behandlet hendelse: {}", minHendelse)
    }
}
```

### 3. Publiser hendelser i service

```kotlin
package com.example.service

import no.nav.ung.deltakelseopplyser.outbox.OutboxProducer
import com.example.events.MinHendelseEvent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MinService(
    private val outboxProducer: OutboxProducer,
    private val repository: MinRepository
) {
    
    fun opprettNoeNytt(data: String): MinEntity {
        // Utfør forretningslogikk
        val entity = repository.save(MinEntity(data = data))
        
        // Publiser hendelse i samme transaksjon
        val event = MinHendelseEvent(
            aggregateId = entity.id.toString(),
            data = data
        )
        outboxProducer.publishEvent(event)
        
        return entity
    }
    
    fun oppdaterFlereEntiteter(entities: List<MinEntity>) {
        val savedEntities = repository.saveAll(entities)
        
        // Publiser flere hendelser på en gang
        val events = savedEntities.map { entity ->
            MinHendelseEvent(
                aggregateId = entity.id.toString(),
                data = entity.data
            )
        }
        outboxProducer.publishEvents(events)
    }
}
```

## Konfigurering

### application.yml

```yaml
# Outbox konfigurering
outbox:
  enabled: true                      # Aktiver/deaktiver outbox funksjonalitet
  scheduler:
    enabled: true                    # Aktiver/deaktiver scheduler
    process-interval: 5000           # Intervall for prosessering (ms)
    retry-interval: 60000            # Intervall for retry av feilede hendelser (ms)
    cleanup-interval: 3600000        # Intervall for opprydding (ms)
    dead-letter-check-interval: 300000 # Intervall for sjekk av dead letter (ms)
  batch-size: 10                     # Antall hendelser å prosessere per batch
  cleanup-retention-days: 7          # Hvor lenge prosesserte hendelser skal beholdes
```

### Spring Boot Properties

Outbox-modulen bruker Spring Boot auto-konfigurering og vil automatisk aktiveres når den er på classpath. Du kan deaktivere den ved å sette:

```yaml
outbox:
  enabled: false
```

## Hendelse statuser

- **PENDING**: Venter på å bli prosessert
- **PROCESSING**: Under prosessering
- **PROCESSED**: Ferdig prosessert
- **FAILED**: Feilet, vil bli forsøkt på nytt
- **DEAD_LETTER**: Feilet for mange ganger, krever manuell intervensjon

## Retry logikk

- **Eksponentiell backoff**: 2^retry_count minutter
- **Standard maks antall forsøk**: 3
- **Dead letter queue**: Hendelser som når maks antall forsøk markeres som DEAD_LETTER

## Overvåkning og Logging

Scheduleren logger følgende informasjon:
- Antall prosesserte hendelser
- Antall dead letter hendelser som krever manuell intervensjon  
- Antall hendelser som er tilbakestilt for retry
- Feilmeldinger ved prosesseringsproblemer

### Eksempel loggutskrift:
```
INFO  OutboxConsumer - Processing 5 outbox events
INFO  OutboxConsumer - Successfully processed 5/5 events
WARN  OutboxScheduler - Found 2 dead letter events that require manual intervention
```

## Testing

Modulen inkluderer omfattende tester for alle komponenter:

- **OutboxDAOTest**: Tester entity logikk og status-overganger
- **OutboxProducerTest**: Tester publisering av enkelt- og bulk-hendelser
- **OutboxConsumerTest**: Tester prosessering, retry-logikk og feilhåndtering

### Eksempel test:

```kotlin
@Test
fun `should publish event and process successfully`() {
    val event = TestEvent("test-id", "test data")
    
    outboxProducer.publishEvent(event)
    
    val processedCount = outboxConsumer.processEvents()
    assertEquals(1, processedCount)
}
```

## Database Schema

Outbox-tabellen har følgende struktur:

```sql
CREATE TABLE outbox (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type       VARCHAR(255) NOT NULL,
    aggregate_id     VARCHAR(255) NOT NULL,
    payload          JSONB        NOT NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at     TIMESTAMP,
    status           VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    retry_count      INTEGER      NOT NULL DEFAULT 0,
    max_retry_count  INTEGER      NOT NULL DEFAULT 3,
    error_message    TEXT,
    next_retry_at    TIMESTAMP
);
```

## Ytelse og Skalering

### Batch Processing
- Standard batch-størrelse: 10 hendelser
- Konfigurerbar via `outbox.batch-size`
- Reduserer database-belastning

### Indeksering  
- Optimaliserte indekser for effektive spørringer
- Index på `(status, created_at)` for rask prosessering
- Index på `next_retry_at` for retry-logikk

### Cleanup
- Automatisk sletting av gamle prosesserte hendelser
- Standard oppbevaring: 7 dager
- Konfigurerbar via `outbox.cleanup-retention-days`

## Feilsøking

### Vanlige problemer og løsninger:

1. **Hendelser prosesseres ikke**
   - Sjekk at `outbox.scheduler.enabled=true`
   - Verifiser at event handlers er registrert som Spring components
   - Sjekk logger for feilmeldinger

2. **Dead letter hendelser**
   - Sjekk `error_message` kolonne i outbox tabellen
   - Vurder å øke `max_retry_count` for spesifikke event-typer
   - Implementer manuell re-prosessering hvis nødvendig

3. **Høy CPU-bruk**
   - Øk `process-interval` for mindre hyppig prosessering
   - Reduser `batch-size` hvis handler-logikk er tungvint

## Fordeler med outbox pattern

1. **Eventual consistency**: Sikrer at hendelser til slutt blir publisert
2. **Transaksjonell sikkerhet**: Hendelser lagres i samme transaksjon som forretningsdata
3. **Retry mekanisme**: Automatisk retry av feilede hendelser med eksponentiell backoff
4. **Observability**: Komplett sporing av hendelse status og feilhåndtering
5. **Skalerbarhet**: Kan håndtere høy last med batch prosessering
6. **Gjenbrukbarhet**: Selvstendig modul som kan brukes på tvers av prosjekter
7. **Spring Boot integrasjon**: Automatisk konfigurering og enkel bruk

## Arkitektur

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Application   │───▶│  OutboxProducer │───▶│   Outbox DB     │
│    Service      │    │                 │    │     Table       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                       │
                       ┌─────────────────┐    ┌────────▼────────┐
                       │ OutboxScheduler │───▶│ OutboxConsumer  │
                       │   (Timer)       │    │                 │
                       └─────────────────┘    └─────────────────┘
                                                       │
                                              ┌────────▼────────┐
                                              │ Event Handlers  │
                                              │ (Kafka, API,    │
                                              │  Notifications) │
                                              └─────────────────┘
```

## Lisensiering

Dette er en intern NAV-modul utviklet for bruk innenfor NAV's systemer.
