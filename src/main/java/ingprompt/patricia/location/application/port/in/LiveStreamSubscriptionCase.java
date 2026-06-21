package ingprompt.patricia.location.application.port.in;

import java.util.UUID;

public interface LiveStreamSubscriptionCase {
    void onSubscriberJoined(UUID eventId, String sessionId);
}
