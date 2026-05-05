# ufore-varsler

Applikasjon for å håndtere varsler knyttet til uføretrygd i Nav.

## Om applikasjonen

`ufore-varsler` er en Spring Boot-applikasjon skrevet i Kotlin. 

## Kom i gang

### Forutsetninger

- JDK 25
- Gradle (eller bruk den medfølgende `./gradlew`)

### Bygg og kjør

```bash
./gradlew build
./gradlew bootRun
```

### Send en beskjed via Kafka

Applikasjonen eksponerer et endepunkt som produserer et `opprett`-event for varslingstypen `beskjed` til topic `aapen-brukervarsel-v1`.

```bash
curl -X POST http://localhost:8080/api/varsler/beskjed \
  -H 'Content-Type: application/json' \
  -d '{"ident":"12345678901","tekst":"Dette er en beskjed"}'
```

Applikasjonen lytter samtidig på `aapen-varsel-hendelse-v1` og logger mottatte hendelser.

### Kjør tester

```bash
./gradlew test
```

---

## Henvendelser
Spørsmål knyttet til koden kan stilles som issues her på GitHub.

### For Nav-ansatte
Interne henvendelser kan sendes via Slack i kanalen #team-ufore.
