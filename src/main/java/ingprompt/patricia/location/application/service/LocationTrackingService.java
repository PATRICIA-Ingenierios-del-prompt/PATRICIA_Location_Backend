package ingprompt.patricia.location.application.service;

import ingprompt.patricia.location.application.port.in.IncidentLocationCase;
import ingprompt.patricia.location.application.port.in.LocationMaintenanceCase;
import ingprompt.patricia.location.application.port.in.TrackLocationCase;
import ingprompt.patricia.location.application.port.out.LiveLocationStoreOutPort;
import ingprompt.patricia.location.application.port.out.StoredLocationRepositoryOutPort;
import ingprompt.patricia.location.domain.model.GeoPoint;
import ingprompt.patricia.location.domain.model.LiveLocation;
import ingprompt.patricia.location.domain.model.StoredLocation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class LocationTrackingService implements TrackLocationCase, LocationMaintenanceCase, IncidentLocationCase {
    private final LiveLocationStoreOutPort liveStore;
    private final StoredLocationRepositoryOutPort storedRepository;
    private final Duration liveTtl;
    private final Duration routineRetention;

    public LocationTrackingService(LiveLocationStoreOutPort liveStore, StoredLocationRepositoryOutPort storedRepository, @Value("${location.live.ttl-seconds}") long liveTtlSeconds, @Value("${location.storage.routine-ttl-hours}") long routineTtlHours) {
        this.liveStore = liveStore;
        this.storedRepository = storedRepository;
        this.liveTtl = Duration.ofSeconds(liveTtlSeconds);
        this.routineRetention = Duration.ofHours(routineTtlHours);
    }

    @Override
    public void updateLiveLocation(UUID eventId, UUID userId, double latitude, double longitude) {
        GeoPoint point = new GeoPoint(latitude, longitude); // validates range
        liveStore.save(new LiveLocation(eventId, userId, point, Instant.now()), liveTtl);
    }

    @Override
    public List<LiveLocation> liveSnapshot(UUID eventId) {
        return liveStore.snapshot(eventId);
    }

    @Override
    public void flushLiveToStorage() {
        for (UUID eventId : liveStore.activeEventIds()) {
            List<StoredLocation> batch = liveStore.snapshot(eventId).stream()
                    .map(live -> StoredLocation.routineFlush(live, routineRetention))
                    .toList();
            if (!batch.isEmpty()) {
                storedRepository.saveAll(batch); // adapter encrypts
                log.info("Flushed {} routine locations for event {}", batch.size(), eventId);
            }
        }
    }

    @Override
    public void purgeExpired() {
        int removed = storedRepository.deleteExpired(Instant.now());
        if (removed > 0) {
            log.info("Purged {} expired location rows past the legal retention window", removed);
        }
    }

    @Override
    public void captureIncidentSnapshot(UUID eventId, UUID reportId) {
        List<StoredLocation> evidence = liveStore.snapshot(eventId).stream()
                .map(live -> StoredLocation.incident(live, reportId))
                .toList();
        if (evidence.isEmpty()) {
            log.warn("Incident {} on event {} captured no live positions (nobody currently tracked)", reportId, eventId);
            return;
        }
        storedRepository.saveAll(evidence); // adapter encrypts; permanent (no TTL)
        log.info("Persisted {} permanent incident locations for event {} (report {})", evidence.size(), eventId, reportId);
    }

    @Override
    public void stopTracking(UUID eventId) {
        liveStore.clearEvent(eventId);
        log.info("Stopped tracking event {} — live data cleared", eventId);
    }
}
