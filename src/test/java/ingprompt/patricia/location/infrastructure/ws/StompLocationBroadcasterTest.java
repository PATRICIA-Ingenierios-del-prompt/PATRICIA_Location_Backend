package ingprompt.patricia.location.infrastructure.ws;

import ingprompt.patricia.location.application.port.out.LiveLocationStoreOutPort;
import ingprompt.patricia.location.domain.model.GeoPoint;
import ingprompt.patricia.location.domain.model.LiveLocation;
import ingprompt.patricia.location.infrastructure.ws.dto.GeoBroadcastMessage;
import ingprompt.patricia.location.infrastructure.ws.dto.GeoSnapshotMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StompLocationBroadcasterTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private LiveLocationStoreOutPort liveStore;

    private StompLocationBroadcaster broadcaster;

    private final UUID eventId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        broadcaster = new StompLocationBroadcaster(messagingTemplate, liveStore);
    }

    private LiveLocation live() {
        return new LiveLocation(eventId, userId, new GeoPoint(4.6, -74.0), Instant.now());
    }

    @Test
    void publishUserPosition_sendsToEventTopic() {
        broadcaster.publishUserPosition(live());
        verify(messagingTemplate).convertAndSend(eq("/topic/geo/" + eventId), any(GeoBroadcastMessage.class));
    }

    @Test
    void seedSubscriber_sendsSnapshotToSession() {
        when(liveStore.snapshot(eventId)).thenReturn(List.of(live()));

        broadcaster.seedSubscriber(eventId, "session-1");

        verify(messagingTemplate).convertAndSendToUser(
                eq("session-1"),
                eq("/queue/geo/" + eventId + "/snapshot"),
                any(GeoSnapshotMessage.class),
                any(java.util.Map.class));
    }
}
