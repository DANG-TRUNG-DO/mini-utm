# Mini UTM

## Local database

The application reads database credentials only from `DB_URL`, `DB_USERNAME`, and
`DB_PASSWORD`. Copy `.env.example` to `.env`, replace `change-me`, then start the
PostGIS database:

```powershell
Copy-Item .env.example .env
docker compose --env-file .env up -d
```

Export the values from `.env` in your shell (Spring Boot does not load `.env`
automatically), activate the local profile, and start the application:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/mini_utm"
$env:DB_USERNAME = "mini_utm"
$env:DB_PASSWORD = "your-local-password"
$env:SPRING_PROFILES_ACTIVE = "local"
.\mvnw.cmd spring-boot:run
```

Flyway creates and validates the schema at startup. Production should provide the
same three database variables through its secret manager and set
`SPRING_PROFILES_ACTIVE=prod`.

## Tests

Docker must be running. Tests use a disposable PostGIS Testcontainer and never use
the developer's local database:

```powershell
.\mvnw.cmd test
```
