package ingprompt.patricia.location.application.port.out;

import ingprompt.patricia.location.domain.model.LiveLocation;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public interface LiveLocationStoreOutPort {
    void save(LiveLocation location, Duration ttl);
    List<LiveLocation> snapshot(UUID eventId);
    java.util.Optional<LiveLocation> findLive(UUID eventId, UUID userId);
    /**
     * Last position ever recorded per user for the event, regardless of the live
     * TTL. Live entries expire after {@code location.live.ttl-seconds} so stale
     * users drop off the map, but the flush/incident paths must still see the
     * final position of users who went silent before the next flush tick.
     */
    List<LiveLocation> lastKnownSnapshot(UUID eventId);
    java.util.Optional<LiveLocation> findLastKnown(UUID eventId, UUID userId);
    Set<UUID> activeEventIds();
    void clearEvent(UUID eventId);
    void registerEvent(UUID eventId, Set<UUID> participants);
    boolean isRegistered(UUID eventId, UUID userId);
}
