package ingprompt.patricia.location.infrastructure.ws;

import ingprompt.patricia.location.application.port.in.LiveStreamSubscriptionCase;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@Component
@AllArgsConstructor
public class StompSubscriptionEventListener {
    private static final Pattern GEO_TOPIC = Pattern.compile("^/topic/geo/([0-9a-fA-F-]{36})$");

    private final LiveStreamSubscriptionCase subscriptionCase;

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }
        Matcher m = GEO_TOPIC.matcher(destination);
        if (!m.matches()) {
            return;
        }
        String sessionId = accessor.getSessionId();
        if (sessionId == null) {
            return;
        }
        UUID eventId = UUID.fromString(m.group(1));
        subscriptionCase.onSubscriberJoined(eventId, sessionId);
    }
}
