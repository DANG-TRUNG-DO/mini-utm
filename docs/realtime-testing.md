# Testing telemetry and alert realtime

The application exposes a native STOMP WebSocket endpoint at:

```text
ws://localhost:8080/ws
```

Telemetry for a drone is published to:

```text
/topic/drones/{droneId}/telemetry
```

Alert lifecycle updates for the same drone are published to:

```text
/topic/drones/{droneId}/alerts
```

## Start the application

Start PostgreSQL and then run the Spring Boot application:

```powershell
docker compose --env-file .env up -d
.\mvnw.cmd spring-boot:run
```

## Subscribe with a small Node.js client

Postman can still be used to send the REST request, but its raw WebSocket
client does not manage STOMP subscriptions. Install a STOMP client in a
temporary folder:

```powershell
npm init -y
npm install @stomp/stompjs ws
```

Create `watch-realtime.mjs`:

```javascript
import { Client } from '@stomp/stompjs';
import WebSocket from 'ws';

const droneId = process.argv[2];
if (!droneId) {
  throw new Error('Usage: node watch-telemetry.mjs <droneId>');
}

const client = new Client({
  webSocketFactory: () => new WebSocket('ws://localhost:8080/ws'),
  reconnectDelay: 2000,
  onConnect: () => {
    const telemetryTopic = `/topic/drones/${droneId}/telemetry`;
    const alertTopic = `/topic/drones/${droneId}/alerts`;
    console.log(`Subscribed to ${telemetryTopic} and ${alertTopic}`);
    client.subscribe(telemetryTopic, frame => {
      console.log('TELEMETRY', JSON.parse(frame.body));
    });
    client.subscribe(alertTopic, frame => {
      console.log('ALERT', JSON.parse(frame.body));
    });
  },
  onStompError: frame => console.error(frame.headers.message, frame.body)
});

client.activate();
```

Run it with the ID of an existing drone:

```powershell
node watch-realtime.mjs <droneId>
```

## Ingest telemetry with Postman

Send `POST http://localhost:8080/api/v1/telemetry` with a JSON body. The
`droneId` must be the same ID used by the subscriber and `recordedAt` must be
unique for that drone.

```json
{
  "droneId": "<droneId>",
  "longitude": 106.700,
  "latitude": 10.780,
  "altitudeM": 75.50,
  "speedMps": 12.345,
  "headingDegrees": 181.50,
  "batteryPercent": 82.25,
  "recordedAt": "2026-07-01T03:00:00Z"
}
```

After the REST request returns `201 Created`, the subscriber prints the
telemetry message. Set `batteryPercent` to `20` or lower to also receive an open
`LOW_BATTERY` alert. Send a later sample with `batteryPercent` at `25` or higher
to receive the same alert with status `RESOLVED`. A subscriber for another drone
ID must not receive either message stream.
