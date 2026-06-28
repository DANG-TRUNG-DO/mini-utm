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
$env:DB_URL = "jdbc:postgresql://localhost:5433/mini-utm"
$env:DB_USERNAME = "postgres"
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

## Drone API

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/v1/drones` | Register a drone |
| `GET` | `/api/v1/drones` | List drones |
| `GET` | `/api/v1/drones/{id}` | Get one drone |
| `PATCH` | `/api/v1/drones/{id}` | Update name or model |
| `PATCH` | `/api/v1/drones/{id}/status` | Change lifecycle status |

New drones start as `INACTIVE`. Valid statuses are `ACTIVE`, `INACTIVE`, and
`MAINTENANCE`. A drone in maintenance must return to `INACTIVE` before it can be
activated again. Serial numbers are trimmed and normalized to uppercase.
