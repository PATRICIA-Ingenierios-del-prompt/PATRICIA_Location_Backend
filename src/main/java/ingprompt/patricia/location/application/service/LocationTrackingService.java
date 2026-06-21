package ingprompt.patricia.location.application.service;

import ingprompt.patricia.location.application.port.in.LiveStreamSubscriptionCase;
import ingprompt.patricia.location.application.port.in.LocationMaintenanceCase;
import ingprompt.patricia.location.application.port.in.TrackLocationCase;
import ingprompt.patricia.location.application.port.in.TrackingLifecycleCase;
import ingprompt.patricia.location.application.port.out.LiveLocationStoreOutPort;
import ingprompt.patricia.location.application.port.out.LocationBroadcasterPort;
import ingprompt.patricia.location.application.port.out.StoredLocationRepositoryOutPort;
import ingprompt.patricia.location.domain.exception.UserNotRegisteredForEventException;
import ingprompt.patricia.location.domain.model.GeoPoint;
import ingprompt.patricia.location.domain.model.LiveLocation;
import ingprompt.patricia.location.domain.model.StoredLocation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class LocationTrackingService implements TrackLocationCase, LocationMaintenanceCase, TrackingLifecycleCase, LiveStreamSubscriptionCase {
    private final LiveLocationStoreOutPort liveStore;
    private final StoredLocationRepositoryOutPort storedRepository;
    private final LocationBroadcasterPort broadcaster;
    private final Duration liveTtl;
    private final Duration routineRetention;

    public LocationTrackingService(LiveLocationStoreOutPort liveStore, StoredLocationRepositoryOutPort storedRepository, LocationBroadcasterPort broadcaster, @Value("${location.live.ttl-seconds}") long liveTtlSeconds, @Value("${location.storage.routine-ttl-hours}") long routineTtlHours) {
        this.liveStore = liveStore;
        this.storedRepository = storedRepository;
        this.broadcaster = broadcaster;
        this.liveTtl = Duration.ofSeconds(liveTtlSeconds);
        this.routineRetention = Duration.ofHours(routineTtlHours);
    }

    @Override
    public void updateLiveLocation(UUID eventId, UUID userId, double latitude, double longitude) {
        if (!liveStore.isRegistered(eventId, userId)) {
            throw new UserNotRegisteredForEventException();
        }
        GeoPoint point = new GeoPoint(latitude, longitude); // validates range
        LiveLocation live = new LiveLocation(eventId, userId, point, Instant.now());
        // 1) Persist to Redis exactly as before (encrypted at the adapter boundary).
        liveStore.save(live, liveTtl);
        // 2) Fan-out to STOMP subscribers. Fire-and-forget; failure to broadcast
        //    must NOT roll back the Redis write — the live map can lag, persistence cannot.
        broadcaster.publishUserPosition(live);
    }

    @Override
    public List<LiveLocation> liveSnapshot(UUID eventId) {
        return liveStore.snapshot(eventId);
    }

    @Override
    public void flushLiveToStorage() {
        for (UUID eventId : liveStore.activeEventIds()) {
            flushEvent(eventId);
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
    public void startTracking(UUID eventId, Set<UUID> participants) {
        liveStore.registerEvent(eventId, participants);
        log.info("Started tracking event {} with {} participants", eventId, participants == null ? 0 : participants.size());
    }

    @Override
    public void stopTracking(UUID eventId) {
        flushEvent(eventId);
        liveStore.clearEvent(eventId);
        log.info("Stopped tracking event {} — final snapshot flushed and live data cleared", eventId);
    }

    @Override
    public void captureIncidentSnapshot(UUID eventId, UUID reportId, UUID reporterId) {
        Optional<LiveLocation> reporterLive = liveStore.findLive(eventId, reporterId);
        if (reporterLive.isEmpty()) {
            log.warn("Incident {} on event {}: reporter {} has no live position — no evidence persisted",
                    reportId, eventId, reporterId);
            return;
        }
        storedRepository.save(StoredLocation.incident(reporterLive.get(), reportId));
        log.info("Persisted permanent incident location for reporter {} on event {} (report {})",
                reporterId, eventId, reportId);
    }

    @Override
    public void onSubscriberJoined(UUID eventId, String sessionId) {
        broadcaster.seedSubscriber(eventId, sessionId);
    }

    private void flushEvent(UUID eventId) {
        for (LiveLocation live : liveStore.snapshot(eventId)) {
            storedRepository.upsertRoutineLastKnown(StoredLocation.routineFlush(live, routineRetention));
        }
    }
}
