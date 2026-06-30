# Mini UTM

Mini UTM is a Spring Boot MVP for managing drones, flight missions, geofences,
telemetry and operational alerts. PostgreSQL/PostGIS stores spatial data, Flyway
manages the schema, and STOMP over WebSocket delivers telemetry and alert updates.

## Requirements

- Java 17
- Docker Desktop with Docker Compose

## Run locally

Create the local environment file and start PostGIS:

```powershell
Copy-Item .env.example .env
docker compose --env-file .env up -d
docker compose ps
```

The application imports `.env` when its working directory is the project root.
Start it with Maven:

```powershell
.\mvnw.cmd spring-boot:run
```

Alternatively, run `MiniUtmApplication` from IntelliJ with the working directory
set to this project directory. Flyway applies and validates the schema during
startup. The health endpoint is available at:

```text
http://localhost:8080/actuator/health
```

## Tests

Docker must be running. Tests use a disposable PostGIS Testcontainer and never
connect to the developer database:

```powershell
.\mvnw.cmd clean test
```

Run only the complete MVP acceptance flow with:

```powershell
.\mvnw.cmd -Dtest=MvpAcceptanceIntegrationTests test
```

## REST API

### Drone

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/v1/drones` | Register a drone |
| `GET` | `/api/v1/drones` | List drones |
| `GET` | `/api/v1/drones/{id}` | Get a drone |
| `PATCH` | `/api/v1/drones/{id}` | Update drone details |
| `PATCH` | `/api/v1/drones/{id}/status` | Change drone status |

Drone statuses are `ACTIVE`, `INACTIVE` and `MAINTENANCE`.

### Geofence

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/v1/geofences` | Create a GeoJSON polygon geofence |
| `GET` | `/api/v1/geofences` | List geofences |
| `GET` | `/api/v1/geofences/{id}` | Get a geofence |
| `PATCH` | `/api/v1/geofences/{id}` | Partially update a geofence |
| `DELETE` | `/api/v1/geofences/{id}` | Delete a geofence |
| `POST` | `/api/v1/geofences/check` | Check whether a point is restricted |

### Mission

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/v1/missions` | Create a mission and flight plan |
| `GET` | `/api/v1/missions` | List missions |
| `GET` | `/api/v1/missions/{id}` | Get a mission |
| `POST` | `/api/v1/missions/{id}/approve` | Approve a planned mission |
| `POST` | `/api/v1/missions/{id}/start` | Start an approved mission |
| `POST` | `/api/v1/missions/{id}/complete` | Complete an active mission |
| `POST` | `/api/v1/missions/{id}/cancel` | Cancel a non-terminal mission |

Mission statuses are `PLANNED`, `APPROVED`, `ACTIVE`, `COMPLETED` and
`CANCELLED`.

### Telemetry and alerts

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/v1/telemetry` | Ingest one telemetry sample |
| `GET` | `/api/v1/drones/{droneId}/telemetry` | Query paginated telemetry history |
| `GET` | `/api/v1/alerts` | Query and filter alerts |
| `GET` | `/api/v1/alerts/{id}` | Get an alert |
| `POST` | `/api/v1/alerts/{id}/acknowledge` | Acknowledge an open alert |
| `POST` | `/api/v1/alerts/{id}/resolve` | Resolve an active alert |

The MVP detects geofence violations, low battery, telemetry loss and route
deviation. Active alert occurrences are deduplicated and refreshed until the
condition recovers.

## Realtime topics

Connect using STOMP at `ws://localhost:8080/ws` and subscribe to:

```text
/topic/drones/{droneId}/telemetry
/topic/drones/{droneId}/alerts
```

See [docs/realtime-testing.md](docs/realtime-testing.md) for a test client and
[docs/mvp-demo.md](docs/mvp-demo.md) for a Postman-ready MVP walkthrough.

## Configuration

Local defaults are documented in `.env.example`. Production should inject
database credentials through its secret manager and set
`SPRING_PROFILES_ACTIVE=prod`. Important optional settings include telemetry
retention, WebSocket origins, low-battery thresholds, telemetry-loss timeout and
route-corridor width.
