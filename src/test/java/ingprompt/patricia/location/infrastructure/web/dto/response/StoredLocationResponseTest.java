package ingprompt.patricia.location.infrastructure.web.dto.response;

import ingprompt.patricia.location.domain.enums.PersistenceReason;
import ingprompt.patricia.location.domain.model.GeoPoint;
import ingprompt.patricia.location.domain.model.StoredLocation;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StoredLocationResponseTest {

    @Test
    void fromMapsRoutineFlush() {
        UUID id = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant recordedAt = Instant.parse("2026-06-15T12:00:00Z");
        Instant expiresAt = recordedAt.plus(Duration.ofHours(12));

        StoredLocation stored = StoredLocation.rehydrate(
                id, eventId, userId, new GeoPoint(4.6097, -74.0817),
                recordedAt, expiresAt, PersistenceReason.ROUTINE_FLUSH, null);

        StoredLocationResponse response = StoredLocationResponse.from(stored);

        assertEquals(id, response.getId());
        assertEquals(eventId, response.getEventId());
        assertEquals(userId, response.getUserId());
        assertEquals(4.6097, response.getLatitude());
        assertEquals(-74.0817, response.getLongitude());
        assertEquals(recordedAt, response.getRecordedAt());
        assertEquals(expiresAt, response.getExpiresAt());
        assertEquals(PersistenceReason.ROUTINE_FLUSH, response.getReason());
        assertNull(response.getReportId());
    }

    @Test
    void fromMapsIncidentReport() {
        UUID reportId = UUID.randomUUID();
        StoredLocation stored = StoredLocation.rehydrate(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new GeoPoint(1.0, 2.0), Instant.now(), null,
                PersistenceReason.INCIDENT_REPORT, reportId);

        StoredLocationResponse response = StoredLocationResponse.from(stored);

        assertEquals(PersistenceReason.INCIDENT_REPORT, response.getReason());
        assertEquals(reportId, response.getReportId());
        assertNull(response.getExpiresAt());
    }
}
