package ingprompt.patricia.location.domain.model;

import java.time.Instant;
import java.util.UUID;


public record LiveLocation(UUID eventId, UUID userId, GeoPoint point, Instant recordedAt) {
}
