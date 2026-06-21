package ingprompt.patricia.location.application.port.out;

import ingprompt.patricia.location.domain.model.StoredLocation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface StoredLocationRepositoryOutPort {
    void save(StoredLocation location);
    void saveAll(List<StoredLocation> locations);

    /**
     * Upserts one routine "last-known" row per {@code (eventId, userId)}. If a routine
     * row already exists for the pair it is updated (coords, recordedAt, expiresAt);
     * otherwise inserted. Routine retention is "last-known only", never a timeline.
     */
    void upsertRoutineLastKnown(StoredLocation location);

    List<StoredLocation> findDecryptedByEventId(UUID eventId);
    int deleteExpired(Instant now);
}
