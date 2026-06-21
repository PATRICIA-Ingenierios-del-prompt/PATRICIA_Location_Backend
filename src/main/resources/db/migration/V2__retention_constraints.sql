-- Change 5: move critical retention rules into the database itself so application
-- bugs or future code paths cannot violate them.

-- 1) Dedupe any pre-existing routine duplicates before applying the unique index.
--    For each (event_id, user_id), keep the newest by recorded_at; drop the rest.
DELETE FROM stored_locations s
 USING stored_locations newer
 WHERE s.reason = 'ROUTINE_FLUSH'
   AND newer.reason = 'ROUTINE_FLUSH'
   AND s.event_id = newer.event_id
   AND s.user_id  = newer.user_id
   AND (s.recorded_at < newer.recorded_at
        OR (s.recorded_at = newer.recorded_at AND s.id <> newer.id AND s.id < newer.id));

-- 2) Partial unique index: enforce one ROUTINE_FLUSH row per (event_id, user_id).
--    Incident rows are unaffected by this constraint.
CREATE UNIQUE INDEX uq_stored_locations_routine_event_user
    ON stored_locations (event_id, user_id)
    WHERE reason = 'ROUTINE_FLUSH';

-- 3) Retention semantics enforced at the row level:
--    - INCIDENT_REPORT rows: must be permanent (expires_at NULL) and carry a report_id.
--    - ROUTINE_FLUSH    rows: must have an expiry and must NOT carry a report_id.
ALTER TABLE stored_locations
    ADD CONSTRAINT ck_stored_locations_retention CHECK (
        (reason = 'INCIDENT_REPORT' AND report_id IS NOT NULL AND expires_at IS NULL)
     OR (reason = 'ROUTINE_FLUSH'   AND report_id IS NULL     AND expires_at IS NOT NULL)
    );
