# MVP demo with Postman

This walkthrough exercises the same business flow as
`MvpAcceptanceIntegrationTests`. Use `http://localhost:8080` as `baseUrl` and
save returned IDs as Postman variables `droneId` and `missionId`.

## 1. Register and activate a drone

`POST {{baseUrl}}/api/v1/drones`

```json
{
  "serialNumber": "DEMO-UAV-001",
  "name": "Demo drone",
  "model": "Quad-X"
}
```

Copy the response `id` to `droneId`, then call
`PATCH {{baseUrl}}/api/v1/drones/{{droneId}}/status`:

```json
{
  "status": "ACTIVE"
}
```

## 2. Create and start a mission

`POST {{baseUrl}}/api/v1/missions`

```json
{
  "droneId": "{{droneId}}",
  "name": "DEMO-FLIGHT",
  "plannedStartAt": "2030-07-01T01:00:00Z",
  "plannedEndAt": "2030-07-01T02:00:00Z",
  "waypoints": [
    {"longitude": 106.700, "latitude": 10.780, "altitudeM": 70.00},
    {"longitude": 106.710, "latitude": 10.790, "altitudeM": 80.00}
  ]
}
```

Copy the response `id` to `missionId`, then call these endpoints in order:

```text
POST {{baseUrl}}/api/v1/missions/{{missionId}}/approve
POST {{baseUrl}}/api/v1/missions/{{missionId}}/start
```

## 3. Create a restricted geofence

Create this zone after starting the mission so the demo can show a runtime
violation. Mission creation correctly rejects a plan that already intersects an
active geofence.

`POST {{baseUrl}}/api/v1/geofences`

```json
{
  "name": "DEMO-RESTRICTED-ZONE",
  "description": "Runtime demo zone",
  "boundary": {
    "type": "Polygon",
    "coordinates": [[
      [106.700, 10.780],
      [106.702, 10.780],
      [106.702, 10.782],
      [106.700, 10.782],
      [106.700, 10.780]
    ]]
  },
  "minAltitudeM": 50.00,
  "maxAltitudeM": 100.00,
  "active": true,
  "validFrom": "2030-07-01T00:00:00Z",
  "validUntil": "2030-07-01T03:00:00Z"
}
```

## 4. Trigger alerts

`POST {{baseUrl}}/api/v1/telemetry`

```json
{
  "droneId": "{{droneId}}",
  "missionId": "{{missionId}}",
  "longitude": 106.7005,
  "latitude": 10.7805,
  "altitudeM": 75.00,
  "speedMps": 12.00,
  "headingDegrees": 45.00,
  "batteryPercent": 15.00,
  "recordedAt": "2030-07-01T01:10:00Z"
}
```

The request creates both `LOW_BATTERY` and `GEOFENCE_VIOLATION` alerts. Query
them with:

```text
GET {{baseUrl}}/api/v1/alerts?droneId={{droneId}}&status=OPEN
```

## 5. Recover and complete the mission

Send a new sample outside the geofence, on the planned path, with a recovered
battery. The `recordedAt` value must be unique per drone.

`POST {{baseUrl}}/api/v1/telemetry`

```json
{
  "droneId": "{{droneId}}",
  "missionId": "{{missionId}}",
  "longitude": 106.705,
  "latitude": 10.785,
  "altitudeM": 75.00,
  "speedMps": 12.00,
  "headingDegrees": 45.00,
  "batteryPercent": 30.00,
  "recordedAt": "2030-07-01T01:10:10Z"
}
```

Verify recovery and finish the flight:

```text
GET  {{baseUrl}}/api/v1/alerts?droneId={{droneId}}&status=RESOLVED
GET  {{baseUrl}}/api/v1/drones/{{droneId}}/telemetry
POST {{baseUrl}}/api/v1/missions/{{missionId}}/complete
```

For live telemetry and alert subscriptions, follow
[realtime-testing.md](realtime-testing.md). Raw WebSocket mode in Postman does
not perform STOMP subscriptions.
