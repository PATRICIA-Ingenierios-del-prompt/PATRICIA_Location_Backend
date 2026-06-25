package ingprompt.patricia.location.infrastructure.ws.security;

import ingprompt.patricia.location.application.port.out.LiveLocationStoreOutPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.security.Principal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StompSubscriptionAuthInterceptorTest {

    @Mock
    private LiveLocationStoreOutPort liveStore;
    @Mock
    private MessageChannel channel;

    private StompSubscriptionAuthInterceptor interceptor;

    private final UUID eventId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        interceptor = new StompSubscriptionAuthInterceptor(liveStore);
    }

    private Message<byte[]> subscribe(String destination, Principal user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setSessionId("session-1");
        if (user != null) {
            accessor.setUser(user);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Principal principal(String name) {
        return () -> name;
    }

    @Test
    void subscribe_whenRegistered_isAdmitted() {
        when(liveStore.isRegistered(eventId, userId)).thenReturn(true);
        Message<byte[]> message = subscribe("/topic/geo/" + eventId, principal(userId.toString()));

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }

    @Test
    void subscribe_whenNotRegistered_isRejected() {
        when(liveStore.isRegistered(eventId, userId)).thenReturn(false);
        Message<byte[]> message = subscribe("/topic/geo/" + eventId, principal(userId.toString()));

        assertThat(interceptor.preSend(message, channel)).isNull();
    }

    @Test
    void subscribe_withoutPrincipal_isRejected() {
        Message<byte[]> message = subscribe("/topic/geo/" + eventId, null);

        assertThat(interceptor.preSend(message, channel)).isNull();
    }

    @Test
    void subscribe_withNonUuidPrincipal_isRejected() {
        Message<byte[]> message = subscribe("/topic/geo/" + eventId, principal("not-a-uuid"));

        assertThat(interceptor.preSend(message, channel)).isNull();
    }

    @Test
    void subscribe_toNonGeoDestination_passesThrough() {
        Message<byte[]> message = subscribe("/topic/other/" + eventId, principal(userId.toString()));

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }
}
