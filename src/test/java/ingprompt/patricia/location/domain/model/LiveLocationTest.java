package ingprompt.patricia.location.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LiveLocationTest {

    @Test
    void recordFieldsAreAccessible() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        GeoPoint point = new GeoPoint(4.6097, -74.0817);
        Instant recordedAt = Instant.now();

        LiveLocation live = new LiveLocation(eventId, userId, point, recordedAt);

        assertEquals(eventId, live.eventId());
        assertEquals(userId, live.userId());
        assertEquals(point, live.point());
        assertEquals(recordedAt, live.recordedAt());
    }

    @Test
    void equalityForSameData() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        GeoPoint point = new GeoPoint(0, 0);
        Instant recordedAt = Instant.parse("2026-01-01T00:00:00Z");

        LiveLocation a = new LiveLocation(eventId, userId, point, recordedAt);
        LiveLocation b = new LiveLocation(eventId, userId, point, recordedAt);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void inequalityForDifferentUsers() {
        UUID eventId = UUID.randomUUID();
        GeoPoint point = new GeoPoint(0, 0);
        Instant recordedAt = Instant.now();

        LiveLocation a = new LiveLocation(eventId, UUID.randomUUID(), point, recordedAt);
        LiveLocation b = new LiveLocation(eventId, UUID.randomUUID(), point, recordedAt);

        assertNotEquals(a, b);
    }
}
