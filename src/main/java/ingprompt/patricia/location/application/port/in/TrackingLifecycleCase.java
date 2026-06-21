package ingprompt.patricia.location.application.port.in;

import java.util.Set;
import java.util.UUID;

public interface TrackingLifecycleCase {
    void startTracking(UUID eventId, Set<UUID> participants);
    void stopTracking(UUID eventId);
    void captureIncidentSnapshot(UUID eventId, UUID reportId, UUID reporterId);
}
