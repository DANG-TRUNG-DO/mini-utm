ALTER TABLE alerts
    ADD COLUMN dedup_key VARCHAR(200),
    ADD COLUMN last_detected_at TIMESTAMPTZ,
    ADD COLUMN occurrence_count INTEGER NOT NULL DEFAULT 1;

UPDATE alerts
SET dedup_key = LOWER(type) || ':' || id::text,
    last_detected_at = detected_at;

ALTER TABLE alerts
    ALTER COLUMN dedup_key SET NOT NULL,
    ALTER COLUMN last_detected_at SET NOT NULL,
    ADD CONSTRAINT ck_alerts_dedup_key CHECK (BTRIM(dedup_key) <> ''),
    ADD CONSTRAINT ck_alerts_last_detected CHECK (last_detected_at >= detected_at),
    ADD CONSTRAINT ck_alerts_occurrence_count CHECK (occurrence_count > 0);

CREATE UNIQUE INDEX uk_alerts_active_dedup
    ON alerts (drone_id, type, dedup_key)
    WHERE status <> 'RESOLVED';
