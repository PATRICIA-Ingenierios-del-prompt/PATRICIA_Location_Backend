-- Persistent location store. Coordinates exist ONLY as AES-256 ciphertext.
CREATE TABLE stored_locations (
    id               UUID         PRIMARY KEY,
    event_id         UUID         NOT NULL,
    user_id          UUID         NOT NULL,
    latitude_cipher  TEXT         NOT NULL,
    longitude_cipher TEXT         NOT NULL,
    recorded_at      TIMESTAMPTZ  NOT NULL,
    expires_at       TIMESTAMPTZ,             -- null => permanent (incident evidence)
    reason           VARCHAR(40)  NOT NULL,
    report_id        UUID
);

CREATE INDEX idx_stored_locations_event_id   ON stored_locations (event_id);
CREATE INDEX idx_stored_locations_expires_at ON stored_locations (expires_at);
CREATE INDEX idx_stored_locations_report_id  ON stored_locations (report_id);

-- Immutable, append-only access log (Ley 1581 audit trail).
CREATE TABLE location_audit_log (
    id           UUID         PRIMARY KEY,
    requester_id UUID         NOT NULL,
    role         VARCHAR(100),
    action       VARCHAR(100) NOT NULL,
    event_id     UUID,
    granted      BOOLEAN      NOT NULL,
    detail       VARCHAR(1000),
    occurred_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_audit_event_id     ON location_audit_log (event_id);
CREATE INDEX idx_audit_requester_id ON location_audit_log (requester_id);
