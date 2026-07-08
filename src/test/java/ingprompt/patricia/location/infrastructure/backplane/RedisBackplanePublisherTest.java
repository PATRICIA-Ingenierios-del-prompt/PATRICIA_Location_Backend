package ingprompt.patricia.location.infrastructure.backplane;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ingprompt.patricia.location.infrastructure.ws.dto.GeoBroadcastMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisBackplanePublisherTest {

    private static final String CHANNEL = "patricia:backplane:location";

    @Mock
    private StringRedisTemplate backplaneRedis;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private GeoBroadcastMessage message() {
        return new GeoBroadcastMessage(UUID.randomUUID(), 4.6, -74.0, Instant.now());
    }

    @Test
    void publish_writesEnvelopeToTheChannel() {
        new RedisBackplanePublisher(backplaneRedis, objectMapper, CHANNEL)
                .publish("/topic/geo/abc", message());

        verify(backplaneRedis).convertAndSend(eq(CHANNEL), contains("\"/topic/geo/abc\""));
    }

    @Test
    void publish_propagatesRedisFailures_soCallerCanFallBackLocally() {
        doThrow(new RedisConnectionFailureException("down"))
                .when(backplaneRedis).convertAndSend(anyString(), any());

        assertThatThrownBy(() ->
                new RedisBackplanePublisher(backplaneRedis, objectMapper, CHANNEL)
                        .publish("/topic/geo/abc", message()))
                .isInstanceOf(RedisConnectionFailureException.class);
    }

    @Test
    void publish_wrapsSerializationErrorsAsProgrammerError() throws Exception {
        ObjectMapper broken = mock(ObjectMapper.class);
        when(broken.valueToTree(any())).thenReturn(objectMapper.createObjectNode());
        when(broken.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {});

        assertThatThrownBy(() ->
                new RedisBackplanePublisher(backplaneRedis, broken, CHANNEL)
                        .publish("/topic/geo/abc", message()))
                .isInstanceOf(IllegalStateException.class);
    }
}
