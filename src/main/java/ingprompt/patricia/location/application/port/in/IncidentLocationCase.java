package ingprompt.patricia.location.application.port.in;

import java.util.UUID;

public interface IncidentLocationCase {
    void captureIncidentSnapshot(UUID eventId, UUID reportId);
    void stopTracking(UUID eventId);
}
