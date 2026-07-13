package ingprompt.patricia.location.application.service;

import ingprompt.patricia.location.application.port.out.LiveLocationStoreOutPort;
import ingprompt.patricia.location.application.port.out.LocationBroadcasterPort;
import ingprompt.patricia.location.application.port.out.StoredLocationRepositoryOutPort;
import ingprompt.patricia.location.domain.enums.PersistenceReason;
import ingprompt.patricia.location.domain.exception.UserNotRegisteredForEventException;
import ingprompt.patricia.location.domain.model.GeoPoint;
import ingprompt.patricia.location.domain.model.LiveLocation;
import ingprompt.patricia.location.domain.model.StoredLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationTrackingServiceTest {

    @Mock
    private LiveLocationStoreOutPort liveStore;
    @Mock
    private StoredLocationRepositoryOutPort storedRepository;
    @Mock
    private LocationBroadcasterPort broadcaster;

    private LocationTrackingService service;

    private UUID eventId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new LocationTrackingService(liveStore, storedRepository, broadcaster, 300L, 12L);
        eventId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    private LiveLocation live() {
        return new LiveLocation(eventId, userId, new GeoPoint(4.6, -74.0), Instant.now());
    }

    // ---- TrackLocationCase ----

    @Test
    void updateLiveLocation_whenRegistered_savesAndBroadcasts() {
        when(liveStore.isRegistered(eventId, userId)).thenReturn(true);

        service.updateLiveLocation(eventId, userId, 4.6, -74.0);

        verify(liveStore).save(any(LiveLocation.class), eq(Duration.ofSeconds(300)));
        verify(broadcaster).publishUserPosition(any(LiveLocation.class));
    }

    @Test
    void updateLiveLocation_whenNotRegistered_throws() {
        when(liveStore.isRegistered(eventId, userId)).thenReturn(false);

        assertThatThrownBy(() -> service.updateLiveLocation(eventId, userId, 4.6, -74.0))
                .isInstanceOf(UserNotRegisteredForEventException.class);
        verify(liveStore, never()).save(any(), any());
        verify(broadcaster, never()).publishUserPosition(any());
    }

    @Test
    void updateLiveLocation_withInvalidCoordinates_throws() {
        when(liveStore.isRegistered(eventId, userId)).thenReturn(true);

        assertThatThrownBy(() -> service.updateLiveLocation(eventId, userId, 200.0, -74.0))
                .isInstanceOf(IllegalArgumentException.class);
        verify(liveStore, never()).save(any(), any());
    }

    @Test
    void liveSnapshot_whenRegistered_delegates() {
        when(liveStore.isRegistered(eventId, userId)).thenReturn(true);
        when(liveStore.snapshot(eventId)).thenReturn(List.of(live()));
        assertThat(service.liveSnapshot(eventId, userId)).hasSize(1);
    }

    @Test
    void liveSnapshot_whenNotRegistered_throws() {
        when(liveStore.isRegistered(eventId, userId)).thenReturn(false);
        assertThatThrownBy(() -> service.liveSnapshot(eventId, userId))
                .isInstanceOf(UserNotRegisteredForEventException.class);
        verify(liveStore, never()).snapshot(any());
    }

    // ---- LocationMaintenanceCase ----

    @Test
    void flushLiveToStorage_upsertsRoutineForEachActiveEvent() {
        when(liveStore.activeEventIds()).thenReturn(Set.of(eventId));
        when(liveStore.lastKnownSnapshot(eventId)).thenReturn(List.of(live()));

        service.flushLiveToStorage();

        ArgumentCaptor<StoredLocation> captor = ArgumentCaptor.forClass(StoredLocation.class);
        verify(storedRepository).upsertRoutineLastKnown(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo(PersistenceReason.ROUTINE_FLUSH);
        assertThat(captor.getValue().getExpiresAt()).isNotNull();
    }

    @Test
    void purgeExpired_delegatesToRepository() {
        when(storedRepository.deleteExpired(any())).thenReturn(3);
        service.purgeExpired();
        verify(storedRepository).deleteExpired(any(Instant.class));
    }

    // ---- TrackingLifecycleCase ----

    @Test
    void startTracking_registersEvent() {
        Set<UUID> participants = Set.of(userId);
        service.startTracking(eventId, participants);
        verify(liveStore).registerEvent(eventId, participants);
    }

    @Test
    void stopTracking_flushesAndClears() {
        when(liveStore.lastKnownSnapshot(eventId)).thenReturn(List.of(live()));

        service.stopTracking(eventId);

        verify(storedRepository).upsertRoutineLastKnown(any(StoredLocation.class));
        verify(liveStore).clearEvent(eventId);
    }

    @Test
    void captureIncidentSnapshot_whenReporterHasPosition_persistsIncident() {
        UUID reportId = UUID.randomUUID();
        when(liveStore.findLive(eventId, userId)).thenReturn(Optional.of(live()));

        service.captureIncidentSnapshot(eventId, reportId, userId);

        ArgumentCaptor<StoredLocation> captor = ArgumentCaptor.forClass(StoredLocation.class);
        verify(storedRepository).save(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo(PersistenceReason.INCIDENT_REPORT);
        assertThat(captor.getValue().getReportId()).isEqualTo(reportId);
    }

    @Test
    void captureIncidentSnapshot_whenNoPosition_persistsNothing() {
        when(liveStore.findLive(eventId, userId)).thenReturn(Optional.empty());
        when(liveStore.findLastKnown(eventId, userId)).thenReturn(Optional.empty());

        service.captureIncidentSnapshot(eventId, UUID.randomUUID(), userId);

        verify(storedRepository, never()).save(any());
    }

    @Test
    void captureIncidentSnapshot_whenLiveExpired_fallsBackToLastKnown() {
        UUID reportId = UUID.randomUUID();
        when(liveStore.findLive(eventId, userId)).thenReturn(Optional.empty());
        when(liveStore.findLastKnown(eventId, userId)).thenReturn(Optional.of(live()));

        service.captureIncidentSnapshot(eventId, reportId, userId);

        ArgumentCaptor<StoredLocation> captor = ArgumentCaptor.forClass(StoredLocation.class);
        verify(storedRepository).save(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo(PersistenceReason.INCIDENT_REPORT);
        assertThat(captor.getValue().getReportId()).isEqualTo(reportId);
    }

    // ---- LiveStreamSubscriptionCase ----

    @Test
    void onSubscriberJoined_seedsSnapshot() {
        service.onSubscriberJoined(eventId, "session-1");
        verify(broadcaster).seedSubscriber(eventId, "session-1");
    }
}
