package ingprompt.patricia.location.infrastructure.backplane;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ingprompt.patricia.location.infrastructure.ws.dto.GeoBroadcastMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Publica con {@link RedisBackplanePublisher} y reinyecta el mensaje resultante
 * en {@link BackplaneStompRelay}, simulando el viaje por Redis: lo que un pod
 * publica debe llegar al broker local de otro pod con el MISMO destino y payload.
 */
@ExtendWith(MockitoExtension.class)
class BackplaneRoundTripTest {

    private static final String CHANNEL = "patricia:backplane:location";

    @Mock
    private StringRedisTemplate backplaneRedis;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules(); // JavaTimeModule for Instant, like Boot's mapper

    @Test
    void publishedMessage_isRelayedToLocalBrokerWithSameDestinationAndPayload() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String destination = "/topic/geo/" + eventId;
        GeoBroadcastMessage original = new GeoBroadcastMessage(userId, 4.61, -74.08, Instant.parse("2026-07-08T21:00:00Z"));

        // Pod A publica
        new RedisBackplanePublisher(backplaneRedis, objectMapper, CHANNEL).publish(destination, original);
        ArgumentCaptor<String> wire = ArgumentCaptor.forClass(String.class);
        verify(backplaneRedis).convertAndSend(eq(CHANNEL), wire.capture());

        // Pod B recibe (mismo formato de mensaje que entrega Redis)
        new BackplaneStompRelay(messagingTemplate, objectMapper)
                .onMessage(new DefaultMessage(CHANNEL.getBytes(StandardCharsets.UTF_8),
                        wire.getValue().getBytes(StandardCharsets.UTF_8)), null);

        ArgumentCaptor<Object> relayed = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq(destination), relayed.capture());

        JsonNode payload = (JsonNode) relayed.getValue();
        assertThat(payload.get("userId").asText()).isEqualTo(userId.toString());
        assertThat(payload.get("latitude").asDouble()).isEqualTo(4.61);
        assertThat(payload.get("longitude").asDouble()).isEqualTo(-74.08);
    }

    @Test
    void malformedMessage_isDroppedWithoutRelaying() {
        new BackplaneStompRelay(messagingTemplate, objectMapper)
                .onMessage(new DefaultMessage(CHANNEL.getBytes(StandardCharsets.UTF_8),
                        "not-json".getBytes(StandardCharsets.UTF_8)), null);
        verify(messagingTemplate, never()).convertAndSend(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(Object.class));
    }

    @Test
    void envelopeWithoutDestination_isDroppedWithoutRelaying() {
        String json = "{\"destination\":null,\"payload\":{\"x\":1}}";
        new BackplaneStompRelay(messagingTemplate, objectMapper)
                .onMessage(new DefaultMessage(CHANNEL.getBytes(StandardCharsets.UTF_8),
                        json.getBytes(StandardCharsets.UTF_8)), null);
        verify(messagingTemplate, never()).convertAndSend(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(Object.class));
    }

    @Test
    void envelopeWithoutPayload_isDroppedWithoutRelaying() {
        String json = "{\"destination\":\"/topic/geo/x\",\"payload\":null}";
        new BackplaneStompRelay(messagingTemplate, objectMapper)
                .onMessage(new DefaultMessage(CHANNEL.getBytes(StandardCharsets.UTF_8),
                        json.getBytes(StandardCharsets.UTF_8)), null);
        verify(messagingTemplate, never()).convertAndSend(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(Object.class));
    }

    @Test
    void relayFailure_isSwallowed_neverKillsTheListener() throws Exception {
        UUID eventId = UUID.randomUUID();
        String destination = "/topic/geo/" + eventId;
        new RedisBackplanePublisher(backplaneRedis, objectMapper, CHANNEL)
                .publish(destination, new GeoBroadcastMessage(UUID.randomUUID(), 4.6, -74.0, Instant.now()));
        ArgumentCaptor<String> wire = ArgumentCaptor.forClass(String.class);
        verify(backplaneRedis).convertAndSend(eq(CHANNEL), wire.capture());

        org.mockito.Mockito.doThrow(new org.springframework.messaging.MessagingException("broker down"))
                .when(messagingTemplate).convertAndSend(eq(destination), org.mockito.ArgumentMatchers.any(Object.class));

        BackplaneStompRelay relay = new BackplaneStompRelay(messagingTemplate, objectMapper);
        // must not throw
        relay.onMessage(new DefaultMessage(CHANNEL.getBytes(StandardCharsets.UTF_8),
                wire.getValue().getBytes(StandardCharsets.UTF_8)), null);
    }
}
