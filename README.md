# UNG Deltakelse Opplyser

![CI / CD](https://github.com/navikt/ung-deltakelse-opplyser/workflows/Build/badge.svg)
![Alerts](https://github.com/navikt/ung-deltakelse-opplyser/workflows/Alerts/badge.svg)
![CodeQl](https://github.com/navikt/ung-deltakelse-opplyser/workflows/CodeQl/badge.svg)

# Innholdsoversikt

* [1. Kontekst](#1-kontekst)
* [2. Funksjonelle Krav](#2-funksjonelle-krav)
* [3. Begrensninger](#3-begrensninger)
* [4. Programvarearkitektur](#5-programvarearkitektur)
* [5. Kode](#6-kode)
* [6. Data](#7-data)
* [7. Infrastrukturarkitektur](#8-infrastrukturarkitektur)
* [8. Distribusjon av tjenesten (deployment)](#9-distribusjon-av-tjenesten-deployment)
* [9. Utviklingsmiljø](#10-utviklingsmilj)
* [10. Drift og støtte](#11-drift-og-sttte)

# 1. Kontekst
Tjenesten lytter på hendelser om innmelding/utmelding av ungdommer i programmet, lagrer ned opplysningene, oppretter oppgave for deltaker på mine-sider, og sender opplysingene til ung-sak.

# 2. Funksjonelle Krav

Denne tjenesten understøtter behovet for:
- lytte på hendelser om innmelding/utmelding av deltakelse i ungdomsprogrammet,
- lagre ned opplysninger om deltakelse i ungdomsprogrammet, 
- tilgjengeliggjøre opplysningene for veileder, deltaker og ung-sak,
- opprette oppgave for deltaker på mine-sider,
- sende opphør av deltakelse til ung-sak.

# 3. Begrensninger

# 4. Programvarearkitektur

# 5. Kode

# 6. Data

# 7. Infrastrukturarkitektur

## System Context Diagram
```mermaid
flowchart TD
    ung-veileder("ungdomsytelse-veileder") -- Melder inn deltaker --> ung-register("ung-deltakelse-opplyser")
    ung-veileder -- Henter opp deltakelser på deltaker --> ung-register
    ung-register -- Lagrer ned opplysninger --> ung-register-db[("ung-deltakelse-opplyser-db")]
    ung-register -- Sender opphørshendelse --> ung-sak("ung-sak")
    ung-register -- Henter personopplysninger for deltaker --> pdl-api("pdl-api")
    ung-register -. Konsumerer og lagrer ned mottatte søknader .-> mottatt-soknad["ungdomsytelse-soknad-cleanup"]
    ung-deltaker("ungdomsytelse-deltaker") --> |Henter registrete deltakelser| ung-register
    
    ung-sak --> |Henter registrerte deltakelser for deltaker| ung-register
    ung-register-db --> deltakelse-table[["Deltakelse"]] & deltaker-table[["Deltaker"]] & oppgave-table[["Oppgave"]] & søknad-tabel[["Søknad"]]

    mottatt-soknad@{ shape: h-cyl}
     pdl-api:::Sky
    classDef Sky stroke-width:1px, stroke-dasharray:none, stroke:#374D7C, fill:#E2EBFF, color:#374D7C
```
Link til [mermaid](https://mermaid.live/edit#pako:eNqFVMuK2zAU_RWhgdKCNdAHDGgxDK0zFNrSRbpqPAuNdWMby5KQ5JmaJJ_Tf5h9fqxXsZ3Ek4R6JZ1zde65D7yiuZFAOV0q85yXwgXyK800wa_VBXuCSoEE9zajeJWm8V0A5WFPZPQdYYz8AIUXUmlNJKggarwwdrvTcFBUPowabOCjiLFWdX4ncpoyyn4FjQ8JxpHDM0fs9u_lNAelEYlK30Xh8KRBkj6rrnRx5jWTj4vLRpFFrw_nU8xBy95suX1xvoxXfLxP4UU9KONpUvKxylCyBeeNnlhdGjet2krFhK1QdDhdEL0m34z2bQOxA6Yg6tCLxoQgQgDity-1FtH_NSoPMPMmgovXw-9hlisQurUZPWrIaPBkYUaiX5hbsh4K7W06QA9HI16fGekhCfbvrIabikw6tv7PkuBod5pHUw_iUcECq0_3GNb6QN7sRU9C3BCAgyvE05HEzx4Y6KHbkQYV6XkPRDrTvbnpDO5WxJfCAiclyzu16WPGFeCcz-uux3IlvE9hSRAh2BVTA3uuZCj5e_snGREpUM450XFtNIwwv_p48ym9-ZKQZaUUv5p9mH2-v09IbpRxI0kTiqvUiErib2MVk2Y0lNBgcRyPUjjc7kxvME60wcw7nVMeXAsJdaYtSsqXAluZ0NZKESCtcB1FM4ZYoX8b07wKmskqGDeAm39mg6Wb) for å redigere diagrammet.

# 8. Distribusjon av tjenesten (deployment)

Distribusjon av tjenesten er gjort med bruk av Github Actions.
[Ung Deltakelse Opplyser Build](https://github.com/navikt/ung-deltakelse-opplyser/actions/workflows/build-and-deploy.yml)

Push/merge til dev-* branch vil teste, bygge og deploye til testmiljø.
Push/merge til master branch vil teste, bygge og deploye til produksjonsmiljø og testmiljø.

# 9. Utviklingsmiljø

## Forutsetninger

* docker
* Java 21
* Kubectl
* Maven

## Bygge Prosjekt

For å bygge kode, kjør:

```shell script
./mvnw clean install
```

## Teste lokalt med VTP via IntelliJ
Forutsetninger:
- Postgresql kjører via Docker med docker config fra [k9-verdikjede](https://github.com/navikt/k9-verdikjede/blob/master/saksbehandling/docker-compose.yml)
- VTP kjører via Docker med docker config fra [k9-verdikjede](https://github.com/navikt/k9-verdikjede/blob/master/saksbehandling/docker-compose.yml)

Bruk run configuration fra .run/UngDeltakelseOpplyserApplication.run.xml

Alternativt:
1. Lag ny run configuration for UngDeltakelseOpplyserApplication
2. Importer environment variabeler fra dev/vtp.env
3. kjør!

## Verdikjedetester
Ende til ende verdikjede tester som involverer denne appen finnes i [k9-verdikjede](https://github.com/navikt/k9-verdikjede/tree/master/verdikjede/src/test/java/no/nav/k9/sak/ung)


## Registrering og henting av data via api-endepunktene

Applikasjonen er konfigurert swagger-ui for å kunne teste ut endepunktene.
For å kunne teste et endepunkt som krever innlogging, må man hente et tokenx token.
Se [Henting av token](#henting-av-token) for mer info.

#### Henting av token i dev-gcp

1. Åpne [Swagger](https://ung-deltakelse-opplyser.intern.dev.nav.no/swagger-ui/index.html) i nettleseren.
2. Trykk "Authorize" i høyre hjørne.
3. Kopier lenken i modalen åpne i ny fane.
4. Velg "TestId på nivå høyt".
5. Oppgi Personidentifikator på testpersonen du vil hente token for, og trykk "Autentiser".
6. Kopier verdien av feltet "access_token" (tokenet).
7. Gå tilbake til Swagger fanen og lim inn tokenet i feltet "Value" og trykk "Authorize".

# 10. Drift og støtte

## Tilkobling til database
For å koble til databasen i dev-gcp må disse kommandoene kjøres:

### Forberede database
Forberedelse vil forberede postgres-instansen ved å koble til ved hjelp av applikasjonslegitimasjonene og som standard endre tillatelsene på det offentlige skjemaet. Alle IAM-brukere i ditt GCP-prosjekt vil kunne koble til instansen.
Denne operasjonen trenger bare å kjøres én gang for hver postgresql-instans og skjema.

```shell script
nais postgres prepare --context dev-gcp --namespace k9saksbehandling ung-deltakelse-opplyser
```

### Gi tilgang til database
Grant deg selv tilgang til en Postgres-database. Dette gjøres ved midlertidig å legge til brukeren din i listen over brukere som kan administrere Cloud SQL-instansene og opprette en databasebruker med e-posten din.
Denne operasjonen trenger bare å kjøres én gang for hver postgresql-database.

```shell script
nais postgres grant --context dev-gcp --namespace k9saksbehandling ung-deltakelse-opplyser
```

### Koble til database
Oppdater IAM-policyer ved å gi brukeren din en tidsbegrenset sql.cloudsql.instanceUser-rolle, og start deretter en proxy til instansen.

```shell script
nais postgres proxy --context dev-gcp --namespace k9saksbehandling ung-deltakelse-opplyser
```

## Logging

Loggene til tjenesten kan leses på to måter:

### Kibana

For [dev-gcp: https://logs.adeo.no/s/nav-logs-legacy/app/r/s/6HlTM](https://logs.adeo.no/s/nav-logs-legacy/app/r/s/6HlTM)

For [prod-gcp: https://logs.adeo.no/s/nav-logs-legacy/app/r/s/fiRdf](https://logs.adeo.no/s/nav-logs-legacy/app/r/s/fiRdf)

### Kubectl

For dev-gcp:

```shell script
kubectl config use-context dev-gcp
kubectl get pods -n k9saksbehandling | grep ung-deltakelse-opplyser
kubectl logs -f ung-deltakelse-opplyser-<POD-ID> --namespace k9saksbehandling -c ung-deltakelse-opplyser
```

For prod-gcp:

```shell script
kubectl config use-context prod-gcp
kubectl get pods -n k9saksbehandling | grep ung-deltakelse-opplyser
kubectl logs -f ung-deltakelse-opplyser-<POD-ID> --namespace k9saksbehandling -c ung-deltakelse-opplyser
```

## Alarmer

Vi bruker [nais-alerts](https://doc.nais.io/observability/alerts) for å sette opp alarmer. Disse finner man konfigurert
i [nais/alerts.yml](nais/alerts.yml).

## Metrics

## Henvendelser

Spørsmål koden eller prosjekttet kan rettes til team k9saksbehandling på:

* [\#sif-saksbehandling](https://nav-it.slack.com/archives/CNUPK6T39)
