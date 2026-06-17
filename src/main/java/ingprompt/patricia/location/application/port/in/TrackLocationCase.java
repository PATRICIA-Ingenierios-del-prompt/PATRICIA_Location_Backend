package ingprompt.patricia.location.application.port.in;

import ingprompt.patricia.location.domain.model.LiveLocation;

import java.util.List;
import java.util.UUID;

public interface TrackLocationCase {
    void updateLiveLocation(UUID eventId, UUID userId, double latitude, double longitude);
    List<LiveLocation> liveSnapshot(UUID eventId);
}
