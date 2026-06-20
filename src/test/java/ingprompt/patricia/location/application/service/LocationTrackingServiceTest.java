package ingprompt.patricia.location.application.service;

import ingprompt.patricia.location.application.port.out.LiveLocationStoreOutPort;
import ingprompt.patricia.location.application.port.out.StoredLocationRepositoryOutPort;
import ingprompt.patricia.location.domain.enums.PersistenceReason;
import ingprompt.patricia.location.domain.model.GeoPoint;
import ingprompt.patricia.location.domain.model.LiveLocation;
import ingprompt.patricia.location.domain.model.StoredLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationTrackingServiceTest {

    @Mock
    private LiveLocationStoreOutPort liveStore;

    @Mock
    private StoredLocationRepositoryOutPort storedRepository;

    @Captor
    private ArgumentCaptor<LiveLocation> liveCaptor;

    @Captor
    private ArgumentCaptor<Duration> ttlCaptor;

    @Captor
    private ArgumentCaptor<List<StoredLocation>> storedListCaptor;

    private LocationTrackingService service;

    private static final long TTL_SECONDS = 300;
    private static final long ROUTINE_TTL_HOURS = 12;

    @BeforeEach
    void setUp() {
        service = new LocationTrackingService(liveStore, storedRepository, TTL_SECONDS, ROUTINE_TTL_HOURS);
    }

    @Test
    void updateLiveLocationSavesToStore() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        service.updateLiveLocation(eventId, userId, 4.6097, -74.0817);

        verify(liveStore).save(liveCaptor.capture(), ttlCaptor.capture());
        LiveLocation saved = liveCaptor.getValue();
        assertEquals(eventId, saved.eventId());
        assertEquals(userId, saved.userId());
        assertEquals(4.6097, saved.point().latitude());
        assertEquals(-74.0817, saved.point().longitude());
        assertEquals(Duration.ofSeconds(TTL_SECONDS), ttlCaptor.getValue());
    }

    @Test
    void updateLiveLocationRejectsInvalidCoords() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class,
                () -> service.updateLiveLocation(eventId, userId, 91, 0));
        verify(liveStore, never()).save(any(), any());
    }

    @Test
    void liveSnapshotDelegatesToStore() {
        UUID eventId = UUID.randomUUID();
        List<LiveLocation> expected = List.of(
                new LiveLocation(eventId, UUID.randomUUID(), new GeoPoint(1, 1), Instant.now()));
        when(liveStore.snapshot(eventId)).thenReturn(expected);

        List<LiveLocation> result = service.liveSnapshot(eventId);

        assertEquals(expected, result);
        verify(liveStore).snapshot(eventId);
    }

    @Test
    void flushLiveToStorageFlushesAllActiveEvents() {
        UUID event1 = UUID.randomUUID();
        UUID event2 = UUID.randomUUID();
        when(liveStore.activeEventIds()).thenReturn(Set.of(event1, event2));

        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        when(liveStore.snapshot(event1)).thenReturn(List.of(
                new LiveLocation(event1, user1, new GeoPoint(1, 1), Instant.now())));
        when(liveStore.snapshot(event2)).thenReturn(List.of(
                new LiveLocation(event2, user2, new GeoPoint(2, 2), Instant.now())));

        service.flushLiveToStorage();

        verify(storedRepository, times(2)).saveAll(storedListCaptor.capture());
        List<List<StoredLocation>> allBatches = storedListCaptor.getAllValues();
        assertEquals(2, allBatches.size());
        allBatches.forEach(batch -> {
            assertEquals(1, batch.size());
            assertEquals(PersistenceReason.ROUTINE_FLUSH, batch.get(0).getReason());
        });
    }

    @Test
    void flushLiveToStorageSkipsEmptySnapshots() {
        UUID eventId = UUID.randomUUID();
        when(liveStore.activeEventIds()).thenReturn(Set.of(eventId));
        when(liveStore.snapshot(eventId)).thenReturn(List.of());

        service.flushLiveToStorage();

        verify(storedRepository, never()).saveAll(any());
    }

    @Test
    void purgeExpiredDelegatesToRepository() {
        when(storedRepository.deleteExpired(any())).thenReturn(5);

        service.purgeExpired();

        verify(storedRepository).deleteExpired(any(Instant.class));
    }

    @Test
    void purgeExpiredWithZeroRemovedDoesNotFail() {
        when(storedRepository.deleteExpired(any())).thenReturn(0);

        assertDoesNotThrow(() -> service.purgeExpired());
        verify(storedRepository).deleteExpired(any(Instant.class));
    }

    @Test
    void startTrackingRegistersEvent() {
        UUID eventId = UUID.randomUUID();
        Set<UUID> participants = Set.of(UUID.randomUUID(), UUID.randomUUID());

        service.startTracking(eventId, participants);

        verify(liveStore).registerEvent(eventId, participants);
    }

    @Test
    void startTrackingWithNullParticipants() {
        UUID eventId = UUID.randomUUID();

        assertDoesNotThrow(() -> service.startTracking(eventId, null));
        verify(liveStore).registerEvent(eventId, null);
    }

    @Test
    void stopTrackingFlushesAndClears() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LiveLocation live = new LiveLocation(eventId, userId, new GeoPoint(3, 3), Instant.now());
        when(liveStore.snapshot(eventId)).thenReturn(List.of(live));

        service.stopTracking(eventId);

        verify(storedRepository).saveAll(storedListCaptor.capture());
        List<StoredLocation> flushed = storedListCaptor.getValue();
        assertEquals(1, flushed.size());
        assertEquals(PersistenceReason.ROUTINE_FLUSH, flushed.get(0).getReason());
        assertNotNull(flushed.get(0).getExpiresAt());
        verify(liveStore).clearEvent(eventId);
    }

    @Test
    void stopTrackingClearsEvenIfSnapshotEmpty() {
        UUID eventId = UUID.randomUUID();
        when(liveStore.snapshot(eventId)).thenReturn(List.of());

        service.stopTracking(eventId);

        verify(storedRepository, never()).saveAll(any());
        verify(liveStore).clearEvent(eventId);
    }

    @Test
    void captureIncidentSnapshotPersistsWithReportId() {
        UUID eventId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LiveLocation live = new LiveLocation(eventId, userId, new GeoPoint(4, -74), Instant.now());
        when(liveStore.snapshot(eventId)).thenReturn(List.of(live));

        service.captureIncidentSnapshot(eventId, reportId);

        verify(storedRepository).saveAll(storedListCaptor.capture());
        List<StoredLocation> saved = storedListCaptor.getValue();
        assertEquals(1, saved.size());
        assertEquals(PersistenceReason.INCIDENT_REPORT, saved.get(0).getReason());
        assertEquals(reportId, saved.get(0).getReportId());
        assertNull(saved.get(0).getExpiresAt());
    }

    @Test
    void captureIncidentSnapshotNoLivePositions() {
        UUID eventId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();
        when(liveStore.snapshot(eventId)).thenReturn(List.of());

        service.captureIncidentSnapshot(eventId, reportId);

        verify(storedRepository, never()).saveAll(any());
    }

    @Test
    void captureIncidentSnapshotMultipleUsers() {
        UUID eventId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();
        LiveLocation live1 = new LiveLocation(eventId, UUID.randomUUID(), new GeoPoint(1, 1), Instant.now());
        LiveLocation live2 = new LiveLocation(eventId, UUID.randomUUID(), new GeoPoint(2, 2), Instant.now());
        when(liveStore.snapshot(eventId)).thenReturn(List.of(live1, live2));

        service.captureIncidentSnapshot(eventId, reportId);

        verify(storedRepository).saveAll(storedListCaptor.capture());
        List<StoredLocation> saved = storedListCaptor.getValue();
        assertEquals(2, saved.size());
        saved.forEach(s -> {
            assertEquals(PersistenceReason.INCIDENT_REPORT, s.getReason());
            assertEquals(reportId, s.getReportId());
        });
    }
}
