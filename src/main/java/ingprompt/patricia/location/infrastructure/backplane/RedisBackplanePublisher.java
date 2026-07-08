package ingprompt.patricia.location.infrastructure.backplane;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

@Slf4j
public class RedisBackplanePublisher {

    private final StringRedisTemplate backplaneRedis;
    private final ObjectMapper objectMapper;
    private final String channel;

    public RedisBackplanePublisher(StringRedisTemplate backplaneRedis, ObjectMapper objectMapper, String channel) {
        this.backplaneRedis = backplaneRedis;
        this.objectMapper = objectMapper;
        this.channel = channel;
    }

    public void publish(String destination, Object payload) {
        try {
            BackplaneEnvelope envelope = new BackplaneEnvelope(destination, objectMapper.valueToTree(payload));
            backplaneRedis.convertAndSend(channel, objectMapper.writeValueAsString(envelope));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Backplane payload is not serializable: " + payload.getClass(), e);
        }
    }
}
