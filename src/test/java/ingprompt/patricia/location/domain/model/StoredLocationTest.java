package ingprompt.patricia.location.domain.model;

import ingprompt.patricia.location.domain.enums.PersistenceReason;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StoredLocationTest {

    private final UUID eventId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final GeoPoint point = new GeoPoint(4.6097, -74.0817);
    private final Instant now = Instant.now();
    private final LiveLocation live = new LiveLocation(eventId, userId, point, now);

    @Test
    void routineFlushSetsReasonAndExpiry() {
        Duration retention = Duration.ofHours(12);
        StoredLocation stored = StoredLocation.routineFlush(live, retention);

        assertEquals(eventId, stored.getEventId());
        assertEquals(userId, stored.getUserId());
        assertEquals(point, stored.getPoint());
        assertEquals(now, stored.getRecordedAt());
        assertEquals(PersistenceReason.ROUTINE_FLUSH, stored.getReason());
        assertNull(stored.getReportId());
        assertNotNull(stored.getId());
        assertNotNull(stored.getExpiresAt());
        assertTrue(stored.getExpiresAt().isAfter(Instant.now().minus(Duration.ofSeconds(5))));
    }

    @Test
    void incidentSetsReasonAndReportId() {
        UUID reportId = UUID.randomUUID();
        StoredLocation stored = StoredLocation.incident(live, reportId);

        assertEquals(eventId, stored.getEventId());
        assertEquals(userId, stored.getUserId());
        assertEquals(point, stored.getPoint());
        assertEquals(now, stored.getRecordedAt());
        assertEquals(PersistenceReason.INCIDENT_REPORT, stored.getReason());
        assertEquals(reportId, stored.getReportId());
        assertNull(stored.getExpiresAt());
        assertNotNull(stored.getId());
    }

    @Test
    void rehydratePreservesAllFields() {
        UUID id = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(Duration.ofHours(12));

        StoredLocation stored = StoredLocation.rehydrate(
                id, eventId, userId, point, now, expiresAt,
                PersistenceReason.ROUTINE_FLUSH, reportId);

        assertEquals(id, stored.getId());
        assertEquals(eventId, stored.getEventId());
        assertEquals(userId, stored.getUserId());
        assertEquals(point, stored.getPoint());
        assertEquals(now, stored.getRecordedAt());
        assertEquals(expiresAt, stored.getExpiresAt());
        assertEquals(PersistenceReason.ROUTINE_FLUSH, stored.getReason());
        assertEquals(reportId, stored.getReportId());
    }

    @Test
    void rehydrateWithNullOptionalFields() {
        UUID id = UUID.randomUUID();
        StoredLocation stored = StoredLocation.rehydrate(
                id, eventId, userId, point, now, null,
                PersistenceReason.INCIDENT_REPORT, null);

        assertNull(stored.getExpiresAt());
        assertNull(stored.getReportId());
    }

    @Test
    void routineFlushGeneratesUniqueIds() {
        StoredLocation a = StoredLocation.routineFlush(live, Duration.ofHours(1));
        StoredLocation b = StoredLocation.routineFlush(live, Duration.ofHours(1));
        assertNotEquals(a.getId(), b.getId());
    }
}
