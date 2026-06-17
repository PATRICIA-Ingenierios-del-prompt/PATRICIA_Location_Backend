package ingprompt.patricia.location.domain.model;

import ingprompt.patricia.location.domain.enums.PersistenceReason;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;


@Getter
public class StoredLocation {
    private final UUID id;
    private final UUID eventId;
    private final UUID userId;
    private final GeoPoint point;
    private final Instant recordedAt;
    private final Instant expiresAt;
    private final PersistenceReason reason;
    private final UUID reportId;            // set only for INCIDENT_REPORT

    private StoredLocation(UUID id, UUID eventId, UUID userId, GeoPoint point, Instant recordedAt, Instant expiresAt, PersistenceReason reason, UUID reportId) {
        this.id = id;
        this.eventId = eventId;
        this.userId = userId;
        this.point = point;
        this.recordedAt = recordedAt;
        this.expiresAt = expiresAt;
        this.reason = reason;
        this.reportId = reportId;
    }


    public static StoredLocation routineFlush(LiveLocation live, Duration retention) {
        Instant now = Instant.now();
        return new StoredLocation(UUID.randomUUID(), live.eventId(), live.userId(), live.point(), live.recordedAt(), now.plus(retention), PersistenceReason.ROUTINE_FLUSH, null);
    }

    public static StoredLocation incident(LiveLocation live, UUID reportId) {
        return new StoredLocation(UUID.randomUUID(), live.eventId(), live.userId(), live.point(), live.recordedAt(), null, PersistenceReason.INCIDENT_REPORT, reportId);
    }

    public static StoredLocation rehydrate(UUID id, UUID eventId, UUID userId, GeoPoint point, Instant recordedAt, Instant expiresAt, PersistenceReason reason, UUID reportId) {
        return new StoredLocation(id, eventId, userId, point, recordedAt, expiresAt, reason, reportId);
    }
}
