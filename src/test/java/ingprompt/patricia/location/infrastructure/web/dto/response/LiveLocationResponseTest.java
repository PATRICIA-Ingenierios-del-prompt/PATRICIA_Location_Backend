package ingprompt.patricia.location.infrastructure.web.dto.response;

import ingprompt.patricia.location.domain.model.GeoPoint;
import ingprompt.patricia.location.domain.model.LiveLocation;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LiveLocationResponseTest {

    @Test
    void fromMapsAllFields() {
        UUID userId = UUID.randomUUID();
        Instant recordedAt = Instant.parse("2026-06-15T12:00:00Z");
        LiveLocation live = new LiveLocation(
                UUID.randomUUID(), userId, new GeoPoint(4.6097, -74.0817), recordedAt);

        LiveLocationResponse response = LiveLocationResponse.from(live);

        assertEquals(userId, response.getUserId());
        assertEquals(4.6097, response.getLatitude());
        assertEquals(-74.0817, response.getLongitude());
        assertEquals(recordedAt, response.getRecordedAt());
    }

    @Test
    void fromWithOriginCoordinates() {
        LiveLocation live = new LiveLocation(
                UUID.randomUUID(), UUID.randomUUID(), new GeoPoint(0, 0), Instant.now());

        LiveLocationResponse response = LiveLocationResponse.from(live);

        assertEquals(0, response.getLatitude());
        assertEquals(0, response.getLongitude());
    }
}
