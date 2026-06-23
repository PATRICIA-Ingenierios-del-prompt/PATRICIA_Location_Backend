package ingprompt.patricia.location.infrastructure.ws.security;

import ingprompt.patricia.location.application.port.out.LiveLocationStoreOutPort;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@Component
@AllArgsConstructor
public class StompSubscriptionAuthInterceptor implements ChannelInterceptor {
    private static final Pattern GEO_TOPIC = Pattern.compile("^/topic/geo/([0-9a-fA-F-]{36})$");

    private final LiveLocationStoreOutPort liveStore;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message;
        }
        String destination = accessor.getDestination();
        if (destination == null) {
            return message;
        }
        Matcher m = GEO_TOPIC.matcher(destination);
        if (!m.matches()) {
            return message; // not our topic — leave alone
        }

        Principal principal = accessor.getUser();
        if (principal == null) {
            log.warn("Rejecting SUBSCRIBE to {} — no authenticated principal", destination);
            return null; // drop the frame
        }
        UUID userId;
        try {
            userId = UUID.fromString(principal.getName());
        } catch (IllegalArgumentException ex) {
            log.warn("Rejecting SUBSCRIBE to {} — principal '{}' is not a UUID", destination, principal.getName());
            return null;
        }
        UUID eventId = UUID.fromString(m.group(1));

        if (!liveStore.isRegistered(eventId, userId)) {
            log.warn("Rejecting SUBSCRIBE: user {} is not registered for event {}", userId, eventId);
            return null;
        }
        return message;
    }
}
