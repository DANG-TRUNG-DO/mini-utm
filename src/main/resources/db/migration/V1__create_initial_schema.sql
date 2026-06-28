CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE drones (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    serial_number       VARCHAR(100) NOT NULL,
    name                VARCHAR(150) NOT NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'OFFLINE',
    model               VARCHAR(100),
    registered_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_drones_serial_number UNIQUE (serial_number),
    CONSTRAINT ck_drones_status CHECK (status IN ('OFFLINE', 'IDLE', 'IN_MISSION', 'MAINTENANCE', 'LOST'))
);

CREATE TABLE geofences (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(150) NOT NULL,
    description         TEXT,
    boundary            geometry(Polygon, 4326) NOT NULL,
    min_altitude_m      NUMERIC(10, 2),
    max_altitude_m      NUMERIC(10, 2),
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    valid_from          TIMESTAMPTZ,
    valid_until         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_geofences_name UNIQUE (name),
    CONSTRAINT ck_geofences_altitude CHECK (
        min_altitude_m IS NULL OR max_altitude_m IS NULL OR min_altitude_m <= max_altitude_m
    ),
    CONSTRAINT ck_geofences_validity CHECK (
        valid_from IS NULL OR valid_until IS NULL OR valid_from < valid_until
    ),
    CONSTRAINT ck_geofences_boundary_valid CHECK (ST_IsValid(boundary))
);

CREATE TABLE missions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    drone_id            UUID NOT NULL,
    name                VARCHAR(150) NOT NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
    planned_path        geometry(LineStringZ, 4326),
    planned_start_at    TIMESTAMPTZ,
    planned_end_at      TIMESTAMPTZ,
    started_at          TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_missions_drone FOREIGN KEY (drone_id) REFERENCES drones (id) ON DELETE RESTRICT,
    CONSTRAINT uk_missions_drone_name UNIQUE (drone_id, name),
    CONSTRAINT ck_missions_status CHECK (status IN ('PLANNED', 'ACTIVE', 'PAUSED', 'COMPLETED', 'CANCELLED', 'FAILED')),
    CONSTRAINT ck_missions_planned_time CHECK (
        planned_start_at IS NULL OR planned_end_at IS NULL OR planned_start_at < planned_end_at
    )
);

CREATE TABLE telemetry (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    drone_id            UUID NOT NULL,
    mission_id          UUID,
    recorded_at         TIMESTAMPTZ NOT NULL,
    position            geometry(PointZ, 4326) NOT NULL,
    speed_mps           NUMERIC(10, 3),
    heading_degrees     NUMERIC(6, 2),
    battery_percent     NUMERIC(5, 2),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_telemetry_drone FOREIGN KEY (drone_id) REFERENCES drones (id) ON DELETE CASCADE,
    CONSTRAINT fk_telemetry_mission FOREIGN KEY (mission_id) REFERENCES missions (id) ON DELETE SET NULL,
    CONSTRAINT uk_telemetry_drone_recorded_at UNIQUE (drone_id, recorded_at),
    CONSTRAINT ck_telemetry_speed CHECK (speed_mps IS NULL OR speed_mps >= 0),
    CONSTRAINT ck_telemetry_heading CHECK (heading_degrees IS NULL OR (heading_degrees >= 0 AND heading_degrees < 360)),
    CONSTRAINT ck_telemetry_battery CHECK (battery_percent IS NULL OR (battery_percent >= 0 AND battery_percent <= 100))
);

CREATE TABLE alerts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    drone_id            UUID NOT NULL,
    mission_id          UUID,
    geofence_id         UUID,
    type                VARCHAR(50) NOT NULL,
    severity            VARCHAR(20) NOT NULL,
    message             TEXT NOT NULL,
    detected_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    acknowledged_at     TIMESTAMPTZ,
    resolved_at         TIMESTAMPTZ,
    CONSTRAINT fk_alerts_drone FOREIGN KEY (drone_id) REFERENCES drones (id) ON DELETE CASCADE,
    CONSTRAINT fk_alerts_mission FOREIGN KEY (mission_id) REFERENCES missions (id) ON DELETE SET NULL,
    CONSTRAINT fk_alerts_geofence FOREIGN KEY (geofence_id) REFERENCES geofences (id) ON DELETE SET NULL,
    CONSTRAINT ck_alerts_severity CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
    CONSTRAINT ck_alerts_acknowledged CHECK (acknowledged_at IS NULL OR acknowledged_at >= detected_at),
    CONSTRAINT ck_alerts_resolved CHECK (resolved_at IS NULL OR resolved_at >= detected_at)
);

CREATE INDEX idx_drones_status ON drones (status);
CREATE INDEX idx_geofences_active ON geofences (active) WHERE active = TRUE;
CREATE INDEX idx_geofences_boundary_gist ON geofences USING GIST (boundary);
CREATE INDEX idx_missions_drone_id ON missions (drone_id);
CREATE INDEX idx_missions_status ON missions (status);
CREATE INDEX idx_missions_planned_start_at ON missions (planned_start_at);
CREATE INDEX idx_missions_planned_path_gist ON missions USING GIST (planned_path);
CREATE INDEX idx_telemetry_mission_id ON telemetry (mission_id);
CREATE INDEX idx_telemetry_recorded_at ON telemetry (recorded_at DESC);
CREATE INDEX idx_telemetry_position_gist ON telemetry USING GIST (position);
CREATE INDEX idx_alerts_drone_detected_at ON alerts (drone_id, detected_at DESC);
CREATE INDEX idx_alerts_mission_id ON alerts (mission_id);
CREATE INDEX idx_alerts_geofence_id ON alerts (geofence_id);
CREATE INDEX idx_alerts_unresolved ON alerts (severity, detected_at DESC) WHERE resolved_at IS NULL;
