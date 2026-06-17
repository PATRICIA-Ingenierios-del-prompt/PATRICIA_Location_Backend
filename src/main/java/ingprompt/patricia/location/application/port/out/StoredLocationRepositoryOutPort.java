package ingprompt.patricia.location.application.port.out;

import ingprompt.patricia.location.domain.model.StoredLocation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface StoredLocationRepositoryOutPort {
    void save(StoredLocation location);
    void saveAll(List<StoredLocation> locations);
    List<StoredLocation> findDecryptedByEventId(UUID eventId);
    int deleteExpired(Instant now);
}
