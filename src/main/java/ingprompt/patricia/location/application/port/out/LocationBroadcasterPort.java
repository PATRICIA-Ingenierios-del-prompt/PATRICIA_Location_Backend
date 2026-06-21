package ingprompt.patricia.location.application.port.out;

import ingprompt.patricia.location.domain.model.LiveLocation;

import java.util.UUID;


public interface LocationBroadcasterPort {
    void publishUserPosition(LiveLocation location);
    void seedSubscriber(UUID eventId, String sessionId);
}
