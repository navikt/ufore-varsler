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

### Kjør tester

```bash
./gradlew test
```

## Metrikk og helse

Applikasjonen eksponerer helse- og metrikk-endepunkter via Spring Actuator:

- `GET /actuator/health`
- `GET /actuator/prometheus`

---

## Henvendelser
Spørsmål knyttet til koden kan stilles som issues her på GitHub.

### For Nav-ansatte
Interne henvendelser kan sendes via Slack i kanalen #team-ufore.
