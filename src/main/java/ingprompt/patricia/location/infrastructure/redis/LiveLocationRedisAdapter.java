package ingprompt.patricia.location.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import ingprompt.patricia.location.application.port.out.EncryptionPort;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class LiveLocationRedisAdapter implements LiveLocationStoreOutPort {
    // Per-user position key: independent TTL per (event, user), no shared expiry.
    private static final String POSITION_KEY_PREFIX = "loc:position:";
    // Per-event roster of authorized users (UUID strings).
    private static final String ROSTER_KEY_PREFIX = "loc:roster:";
    // Per-event index of users currently tracked, so we can fan-out snapshot/clear.
    private static final String EVENT_USERS_KEY_PREFIX = "loc:event-users:";
    private static final String ACTIVE_EVENTS_KEY = "loc:active-events";
    // Per-event hash userId -> encrypted last position, with NO live TTL: closes the
    // gap where a user goes silent > live TTL before a flush tick and their final
    // position would otherwise never reach Postgres. Cleared on event end; the
    // safety TTL below only guards against a lost event.ended message.
    private static final String LAST_KNOWN_KEY_PREFIX = "loc:last-known:";
    private static final Duration LAST_KNOWN_SAFETY_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;
    private final EncryptionPort encryption;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @SneakyThrows
    public void save(LiveLocation location, Duration ttl) {
        String json = objectMapper.writeValueAsString(new PositionValue(
                location.point().latitude(),
                location.point().longitude(),
                location.recordedAt().toEpochMilli()));
        String cipher = encryption.encrypt(json);

        String key = positionKey(location.eventId(), location.userId());
        redis.opsForValue().set(key, cipher, ttl);

        // Track this user under the event so snapshot/clear stays O(participants).
        // The index entry inherits the position TTL (refreshed on every save).
        String users = eventUsersKey(location.eventId());
        redis.opsForSet().add(users, location.userId().toString());
        redis.expire(users, ttl);

        // Last-known survives the live TTL so flush/incident can persist the final
        // position of users who stopped updating before the next tick.
        String lastKnown = lastKnownKey(location.eventId());
        redis.opsForHash().put(lastKnown, location.userId().toString(), cipher);
        redis.expire(lastKnown, LAST_KNOWN_SAFETY_TTL);

        redis.opsForSet().add(ACTIVE_EVENTS_KEY, location.eventId().toString());
    }

    @Override
    public List<LiveLocation> snapshot(UUID eventId) {
        Set<String> userIds = redis.opsForSet().members(eventUsersKey(eventId));
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        List<LiveLocation> result = new ArrayList<>(userIds.size());
        for (String idStr : userIds) {
            UUID userId = UUID.fromString(idStr);
            findLive(eventId, userId).ifPresentOrElse(result::add, () ->
                    // Position TTL expired but the index entry lingered; clean it up.
                    redis.opsForSet().remove(eventUsersKey(eventId), idStr));
        }
        return result;
    }

    @Override
    public Optional<LiveLocation> findLive(UUID eventId, UUID userId) {
        String cipher = redis.opsForValue().get(positionKey(eventId, userId));
        if (cipher == null) {
            return Optional.empty();
        }
        PositionValue pos = readValue(encryption.decrypt(cipher));
        return Optional.of(new LiveLocation(eventId, userId, new GeoPoint(pos.getLat(), pos.getLng()), Instant.ofEpochMilli(pos.getTs())));
    }

    @Override
    public List<LiveLocation> lastKnownSnapshot(UUID eventId) {
        var entries = redis.opsForHash().entries(lastKnownKey(eventId));
        if (entries.isEmpty()) {
            return List.of();
        }
        List<LiveLocation> result = new ArrayList<>(entries.size());
        entries.forEach((userIdStr, cipher) -> {
            PositionValue pos = readValue(encryption.decrypt((String) cipher));
            result.add(new LiveLocation(eventId, UUID.fromString((String) userIdStr), new GeoPoint(pos.getLat(), pos.getLng()), Instant.ofEpochMilli(pos.getTs())));
        });
        return result;
    }

    @Override
    public Optional<LiveLocation> findLastKnown(UUID eventId, UUID userId) {
        Object cipher = redis.opsForHash().get(lastKnownKey(eventId), userId.toString());
        if (cipher == null) {
            return Optional.empty();
        }
        PositionValue pos = readValue(encryption.decrypt((String) cipher));
        return Optional.of(new LiveLocation(eventId, userId, new GeoPoint(pos.getLat(), pos.getLng()), Instant.ofEpochMilli(pos.getTs())));
    }

    @Override
    public Set<UUID> activeEventIds() {
        Set<String> ids = redis.opsForSet().members(ACTIVE_EVENTS_KEY);
        if (ids == null) {
            return Set.of();
        }
        // An event stays active while EITHER index exists: the users-index expires
        // with the live TTL, but last-known must keep the event flushable until
        // event.ended clears it (or the safety TTL reaps a lost event).
        return ids.stream()
                .filter(id -> Boolean.TRUE.equals(redis.hasKey(eventUsersKey(UUID.fromString(id))))
                        || Boolean.TRUE.equals(redis.hasKey(lastKnownKey(UUID.fromString(id))))
                        || removeFromActive(id))
                .map(UUID::fromString)
                .collect(Collectors.toSet());
    }

    @Override
    public void clearEvent(UUID eventId) {
        // Delete every per-user position key, then the index, the roster, and the active set entry.
        Set<String> userIds = redis.opsForSet().members(eventUsersKey(eventId));
        if (userIds != null) {
            for (String idStr : userIds) {
                redis.delete(positionKey(eventId, UUID.fromString(idStr)));
            }
        }
        redis.delete(eventUsersKey(eventId));
        redis.delete(lastKnownKey(eventId));
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

    @Override
    public boolean isRegistered(UUID eventId, UUID userId) {
        Boolean member = redis.opsForSet().isMember(rosterKey(eventId), userId.toString());
        return Boolean.TRUE.equals(member);
    }

    private boolean removeFromActive(String id) {
        redis.opsForSet().remove(ACTIVE_EVENTS_KEY, id);
        return false;
    }

    @SneakyThrows
    private PositionValue readValue(String json) {
        return objectMapper.readValue(json, PositionValue.class);
    }

    private String positionKey(UUID eventId, UUID userId) {
        return POSITION_KEY_PREFIX + eventId + ":" + userId;
    }

    private String rosterKey(UUID eventId) {
        return ROSTER_KEY_PREFIX + eventId;
    }

    private String eventUsersKey(UUID eventId) {
        return EVENT_USERS_KEY_PREFIX + eventId;
    }

    private String lastKnownKey(UUID eventId) {
        return LAST_KNOWN_KEY_PREFIX + eventId;
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
