package ingprompt.patricia.location.application.port.out;

import ingprompt.patricia.location.domain.model.LiveLocation;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface LiveLocationStoreOutPort {
    void save(LiveLocation location, Duration ttl);
    List<LiveLocation> snapshot(UUID eventId);
    Set<UUID> activeEventIds();
    void clearEvent(UUID eventId);
}
