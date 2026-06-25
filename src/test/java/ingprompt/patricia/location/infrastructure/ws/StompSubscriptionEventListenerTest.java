package ingprompt.patricia.location.infrastructure.ws;

import ingprompt.patricia.location.application.port.in.LiveStreamSubscriptionCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StompSubscriptionEventListenerTest {

    @Mock
    private LiveStreamSubscriptionCase subscriptionCase;
    @InjectMocks
    private StompSubscriptionEventListener listener;

    private final UUID eventId = UUID.randomUUID();

    private SessionSubscribeEvent subscribeTo(String destination, String sessionId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        if (sessionId != null) {
            accessor.setSessionId(sessionId);
        }
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return new SessionSubscribeEvent(this, message);
    }

    @Test
    void onSubscribe_toGeoTopic_seedsSnapshot() {
        listener.onSubscribe(subscribeTo("/topic/geo/" + eventId, "session-1"));
        verify(subscriptionCase).onSubscriberJoined(eventId, "session-1");
    }

    @Test
    void onSubscribe_toOtherTopic_isIgnored() {
        listener.onSubscribe(subscribeTo("/topic/other/" + eventId, "session-1"));
        verifyNoInteractions(subscriptionCase);
    }

    @Test
    void onSubscribe_withoutSessionId_isIgnored() {
        listener.onSubscribe(subscribeTo("/topic/geo/" + eventId, null));
        verifyNoInteractions(subscriptionCase);
    }
}
