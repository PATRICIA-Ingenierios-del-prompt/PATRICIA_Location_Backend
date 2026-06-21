package ingprompt.patricia.location.infrastructure.ws;

import ingprompt.patricia.location.application.port.out.LiveLocationStoreOutPort;
import ingprompt.patricia.location.application.port.out.LocationBroadcasterPort;
import ingprompt.patricia.location.domain.model.LiveLocation;
import ingprompt.patricia.location.infrastructure.ws.dto.GeoBroadcastMessage;
import ingprompt.patricia.location.infrastructure.ws.dto.GeoSnapshotMessage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;


@Slf4j
@Component
@AllArgsConstructor
public class StompLocationBroadcaster implements LocationBroadcasterPort {
    private static final String TOPIC_PREFIX = "/topic/geo/";
    private static final String SNAPSHOT_QUEUE_PREFIX = "/queue/geo/";   // suffixed with eventId/snapshot

    private final SimpMessagingTemplate messagingTemplate;
    private final LiveLocationStoreOutPort liveStore;

    @Override
    public void publishUserPosition(LiveLocation location) {
        try {
            messagingTemplate.convertAndSend(
                    TOPIC_PREFIX + location.eventId(),
                    GeoBroadcastMessage.from(location));
        } catch (RuntimeException ex) {
            // Live map can lag; persistence cannot. Never propagate.
            log.warn("Failed to broadcast position for user {} on event {}: {}",
                    location.userId(), location.eventId(), ex.getMessage());
        }
    }

    @Override
    public void seedSubscriber(UUID eventId, String sessionId) {
        try {
            List<GeoBroadcastMessage> positions = liveStore.snapshot(eventId).stream()
                    .map(GeoBroadcastMessage::from)
                    .toList();
            GeoSnapshotMessage payload = new GeoSnapshotMessage(eventId, positions);

            // Session-scoped delivery: only this session receives the snapshot.
            SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
            headers.setSessionId(sessionId);
            headers.setLeaveMutable(true);
            messagingTemplate.convertAndSendToUser(
                    sessionId,
                    SNAPSHOT_QUEUE_PREFIX + eventId + "/snapshot",
                    payload,
                    createHeaders(sessionId));
        } catch (RuntimeException ex) {
            log.warn("Failed to seed subscriber session {} for event {}: {}", sessionId, eventId, ex.getMessage());
        }
    }

    private MessageHeaders createHeaders(String sessionId) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        accessor.setSessionId(sessionId);
        accessor.setLeaveMutable(true);
        return accessor.getMessageHeaders();
    }
}
