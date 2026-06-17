package ingprompt.patricia.location.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import ingprompt.patricia.location.application.port.out.LiveLocationStoreOutPort;
import ingprompt.patricia.location.domain.model.GeoPoint;
import ingprompt.patricia.location.domain.model.LiveLocation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Component
@AllArgsConstructor
public class LiveLocationRedisAdapter implements LiveLocationStoreOutPort {
    private static final String EVENT_KEY_PREFIX = "loc:event:";
    private static final String ROSTER_KEY_PREFIX = "loc:roster:";
    private static final String ACTIVE_EVENTS_KEY = "loc:active-events";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @SneakyThrows
    public void save(LiveLocation location, Duration ttl) {
        String key = eventKey(location.eventId());
        String json = objectMapper.writeValueAsString(new PositionValue(location.point().latitude(), location.point().longitude(), location.recordedAt().toEpochMilli()));
        redis.opsForHash().put(key, location.userId().toString(), json);
        redis.expire(key, ttl);
        redis.opsForSet().add(ACTIVE_EVENTS_KEY, location.eventId().toString());
    }

    @Override
    public List<LiveLocation> snapshot(UUID eventId) {
        Map<Object, Object> raw = redis.opsForHash().entries(eventKey(eventId));
        List<LiveLocation> result = new ArrayList<>(raw.size());
        for (Map.Entry<Object, Object> entry : raw.entrySet()) {
            UUID userId = UUID.fromString((String) entry.getKey());
            PositionValue pos = readValue((String) entry.getValue());
            result.add(new LiveLocation(eventId, userId, new GeoPoint(pos.getLat(), pos.getLng()), Instant.ofEpochMilli(pos.getTs())));
        }
        return result;
    }

    @Override
    public Set<UUID> activeEventIds() {
        Set<String> ids = redis.opsForSet().members(ACTIVE_EVENTS_KEY);
        if (ids == null) {
            return Set.of();
        }
        // Prune events whose hash already expired so the set doesn't grow unbounded.
        return ids.stream()
                .filter(id -> Boolean.TRUE.equals(redis.hasKey(eventKey(UUID.fromString(id)))) || removeFromActive(id))
                .map(UUID::fromString)
                .collect(Collectors.toSet());
    }

    @Override
    public void clearEvent(UUID eventId) {
        redis.delete(eventKey(eventId));
        redis.delete(rosterKey(eventId));
        redis.opsForSet().remove(ACTIVE_EVENTS_KEY, eventId.toString());
    }

    @Override
    public void registerEvent(UUID eventId, Set<UUID> participants) {
        if (participants == null || participants.isEmpty()) {
            return;
        }
        String[] ids = participants.stream().map(UUID::toString).toArray(String[]::new);
        redis.opsForSet().add(rosterKey(eventId), ids);
    }

    private boolean removeFromActive(String id) {
        redis.opsForSet().remove(ACTIVE_EVENTS_KEY, id);
        return false; // filtered out
    }

    @SneakyThrows
    private PositionValue readValue(String json) {
        return objectMapper.readValue(json, PositionValue.class);
    }

    private String eventKey(UUID eventId) {
        return EVENT_KEY_PREFIX + eventId;
    }

    private String rosterKey(UUID eventId) {
        return ROSTER_KEY_PREFIX + eventId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class PositionValue {
        private double lat;
        private double lng;
        private long ts;
    }
}
